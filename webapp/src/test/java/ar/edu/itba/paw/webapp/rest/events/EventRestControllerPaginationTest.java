package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.service.EventService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventRestControllerPaginationTest {

    private EventRestController controller;
    private EventService eventService;
    private EventDtoAssembler eventDtoAssembler;

    @Before
    public void setUp() {
        controller = new EventRestController();
        eventService = mock(EventService.class);
        eventDtoAssembler = mock(EventDtoAssembler.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getRequestUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/clubs/club/events"));

        ReflectionTestUtils.setField(controller, "eventService", eventService);
        ReflectionTestUtils.setField(controller, "eventDtoAssembler", eventDtoAssembler);
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void listByClubAddsPaginationHeaders() {
        UUID clubId = UUID.randomUUID();
        Event event = new Event(UUID.randomUUID(), clubId, "Event", "Description", LocalDate.now().plusDays(1), null);
        when(eventService.listByClub(clubId, 1, 10)).thenReturn(Collections.singletonList(event));
        when(eventService.countByClub(clubId)).thenReturn(25);
        when(eventDtoAssembler.toDto(any(UriInfo.class), eq(clubId), eq(event))).thenReturn(new EventDto());

        Response response = controller.listByClub(clubId, 1, 10);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("25", response.getHeaderString("X-Total-Count"));
        String link = response.getHeaderString("Link");
        assertTrue(link.contains("rel=\"first\""));
        assertTrue(link.contains("rel=\"prev\""));
        assertTrue(link.contains("rel=\"next\""));
        assertTrue(link.contains("rel=\"last\""));
    }
}
