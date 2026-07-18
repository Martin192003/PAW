package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventPhotoRestControllerTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();

    private EventService eventService;
    private ClubService clubService;
    private AuthenticatedUserResolver authenticatedUserResolver;
    private EventPhotoRestController controller;

    @Before
    public void setUp() {
        eventService = mock(EventService.class);
        clubService = mock(ClubService.class);
        authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
        controller = new EventPhotoRestController();

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));

        ReflectionTestUtils.setField(controller, "eventService", eventService);
        ReflectionTestUtils.setField(controller, "clubService", clubService);
        ReflectionTestUtils.setField(controller, "eventDtoAssembler", new EventDtoAssembler());
        ReflectionTestUtils.setField(controller, "authenticatedUserResolver", authenticatedUserResolver);
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void testPutPhoto_Unauthenticated_ReturnsUnauthorized() throws Exception {
        when(authenticatedUserResolver.resolve(any())).thenReturn(null);

        Response response = controller.putPhoto(CLUB_ID, EVENT_ID, mock(SecurityContext.class));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeletePhoto_Unauthenticated_ReturnsUnauthorized() {
        when(authenticatedUserResolver.resolve(any())).thenReturn(null);

        Response response = controller.deletePhoto(CLUB_ID, EVENT_ID, mock(SecurityContext.class));

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testDeletePhoto_Success_ReturnsNoContent() {
        User admin = user(UUID.randomUUID());
        when(authenticatedUserResolver.resolve(any())).thenReturn(admin);
        Event withoutPhoto = event(EVENT_ID, null);
        when(eventService.getForClub(CLUB_ID, EVENT_ID)).thenReturn(withoutPhoto);

        Response response = controller.deletePhoto(CLUB_ID, EVENT_ID, mock(SecurityContext.class));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertNull(response.getEntity());
    }

    @Test
    public void testGetPhoto_Found_ReturnsNoCacheImage() {
        Photo photo = new Photo();
        photo.setPhotoId(UUID.randomUUID());
        photo.setContentType("image/jpeg");
        photo.setData(new byte[]{1, 2, 3});
        photo.setFilename("evento.jpg");
        photo.setSizeBytes(3L);
        photo.setCreatedAt(OffsetDateTime.now());
        when(eventService.getEventPhoto(CLUB_ID, EVENT_ID)).thenReturn(photo);

        Request request = mock(Request.class);
        // any(EntityTag.class), no any() a secas: evaluatePreconditions está sobrecargado
        // (EntityTag / Date / Date,EntityTag) y el compilador no puede resolver cuál
        // sobrecarga corresponde a un any() sin tipo.
        when(request.evaluatePreconditions(any(EntityTag.class))).thenReturn(null);

        Response response = controller.getPhoto(CLUB_ID, EVENT_ID, request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getHeaderString("Content-Disposition"));
        assertEquals(true, response.getHeaderString("Cache-Control").contains("no-cache"));
    }

    private static Event event(UUID id, UUID photoId) {
        return new Event(id, CLUB_ID, "Fiesta", "desc", LocalDate.now().plusDays(5), photoId);
    }

    private static User user(UUID id) {
        User user = new User(
                id, "user-" + id, "Name", "Surname", id + "@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455", Role.USER, OffsetDateTime.now(), "es"
        );
        user.setVerified(true);
        return user;
    }
}
