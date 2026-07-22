package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PhotoRestControllerTest {

    private static final UUID PHOTO_ID = UUID.randomUUID();

    private PhotoService photoService;
    private ClubService clubService;
    private AuthenticatedUserResolver authenticatedUserResolver;
    private PhotoRestController controller;
    private Request request;

    @Before
    public void setUp() {
        photoService = mock(PhotoService.class);
        clubService = mock(ClubService.class);
        authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
        controller = new PhotoRestController(photoService, clubService, authenticatedUserResolver);
        request = mock(Request.class);
        // Sin condicional (sin If-None-Match del cliente): no hay 304, sigue de largo.
        // any(EntityTag.class) en vez de any() a secas: Request.evaluatePreconditions
        // está sobrecargado (EntityTag / Date / Date,EntityTag) y any() sin tipo es
        // ambiguo para el compilador entre esas sobrecargas.
        when(request.evaluatePreconditions(org.mockito.ArgumentMatchers.any(EntityTag.class))).thenReturn(null);
    }

    @Test
    public void testGetPhoto_InvalidId_ReturnsBadRequest() {
        Response response = controller.getPhoto("not-a-uuid", request);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetPhoto_NotFound_ReturnsNotFound() {
        when(photoService.getWithId(PHOTO_ID)).thenReturn(Optional.empty());
        Response response = controller.getPhoto(PHOTO_ID.toString(), request);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetPhoto_Found_ReturnsConditionalCacheHeaders() {
        Photo photo = new Photo();
        photo.setPhotoId(PHOTO_ID);
        photo.setContentType("image/jpeg");
        photo.setData(new byte[]{1, 2, 3});
        photo.setSizeBytes(3L);
        photo.setCreatedAt(OffsetDateTime.now());
        when(photoService.getWithId(PHOTO_ID)).thenReturn(Optional.of(photo));

        Response response = controller.getPhoto(PHOTO_ID.toString(), request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("image/jpeg", response.getMediaType().toString());
        assertEquals(true, response.getEntityTag() != null);
        // Cache condicional (no cache incondicional para un recurso mutable como una foto).
        assertEquals(true, response.getHeaderString("Cache-Control").contains("no-cache"));
    }

    @Test
    public void testGetPhoto_MatchingEtag_ReturnsNotModified() {
        Photo photo = new Photo();
        photo.setPhotoId(PHOTO_ID);
        photo.setContentType("image/jpeg");
        photo.setData(new byte[]{1, 2, 3});
        photo.setSizeBytes(3L);
        photo.setCreatedAt(OffsetDateTime.now());
        when(photoService.getWithId(PHOTO_ID)).thenReturn(Optional.of(photo));

        Response.ResponseBuilder notModifiedBuilder = Response.status(Response.Status.NOT_MODIFIED);
        when(request.evaluatePreconditions(org.mockito.ArgumentMatchers.any(EntityTag.class))).thenReturn(notModifiedBuilder);

        Response response = controller.getPhoto(PHOTO_ID.toString(), request);

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeletePhoto_Unauthenticated_ReturnsUnauthorized() {
        Photo photo = carouselPhoto();
        when(photoService.getWithId(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        Response response = controller.deletePhoto(PHOTO_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeletePhoto_CanonicalResourceReturnsNoContent() {
        Photo photo = carouselPhoto();
        User user = new User(
                UUID.randomUUID(), "admin", "Name", "Surname", "admin@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455", Role.ADMIN, OffsetDateTime.now(), "es"
        );
        when(photoService.getWithId(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        when(photoService.delete(PHOTO_ID)).thenReturn(true);

        Response response = controller.deletePhoto(PHOTO_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(clubService).assertAdmin(photo.getClubId(), user.getId());
        verify(photoService).delete(PHOTO_ID);
    }

    @Test
    public void testDeletePhoto_LegacyPhotoWithoutTypeReturnsNoContent() {
        Photo photo = carouselPhoto();
        photo.setPhotoType(null);
        User user = new User(
                UUID.randomUUID(), "admin", "Name", "Surname", "admin@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455", Role.ADMIN, OffsetDateTime.now(), "es"
        );
        when(photoService.getWithId(PHOTO_ID)).thenReturn(Optional.of(photo));
        when(authenticatedUserResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(user);
        when(photoService.delete(PHOTO_ID)).thenReturn(true);

        Response response = controller.deletePhoto(PHOTO_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(photoService).delete(PHOTO_ID);
    }

    private static Photo carouselPhoto() {
        Photo photo = new Photo();
        photo.setPhotoId(PHOTO_ID);
        photo.setClubId(UUID.randomUUID());
        photo.setPhotoType(PhotoType.CAROUSEL);
        return photo;
    }
}
