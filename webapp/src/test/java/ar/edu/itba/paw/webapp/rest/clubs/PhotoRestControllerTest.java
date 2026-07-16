package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.service.PhotoService;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PhotoRestControllerTest {

    private static final UUID PHOTO_ID = UUID.randomUUID();

    private PhotoService photoService;
    private PhotoRestController controller;
    private Request request;

    @Before
    public void setUp() {
        photoService = mock(PhotoService.class);
        controller = new PhotoRestController(photoService);
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
}
