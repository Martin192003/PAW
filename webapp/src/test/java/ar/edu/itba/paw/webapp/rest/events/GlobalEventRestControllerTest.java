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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalEventRestController es, junto con EventRestController, la excepción al patrón
 * de colecciones "solo-links" del resto de la API (sigue devolviendo EventDto completos
 * embebidos) -- ver AUDITORIA_ENTREGA_FINAL.md punto 19. No estaba testeado.
 */
public class GlobalEventRestControllerTest {

    private EventService eventService;
    private GlobalEventRestController controller;

    @Before
    public void setUp() {
        eventService = mock(EventService.class);
        controller = new GlobalEventRestController();

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getRequestUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/events"));

        ReflectionTestUtils.setField(controller, "eventService", eventService);
        ReflectionTestUtils.setField(controller, "eventDtoAssembler", new EventDtoAssembler());
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void testListUpcoming_InvalidPaging_ReturnsBadRequest() {
        Response response = controller.listUpcoming(-1, 20);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListUpcoming_ReturnsEmbeddedEventDtos() {
        UUID clubId = UUID.randomUUID();
        Event event = new Event(UUID.randomUUID(), clubId, "Fiesta", "desc", LocalDate.now().plusDays(2), null);
        when(eventService.listUpcoming(0, 20)).thenReturn(List.of(event));
        when(eventService.countUpcoming()).thenReturn(1);
        when(eventService.mapClubNamesForEvents(List.of(event.getEventId())))
                .thenReturn(Map.of(event.getEventId(), "Mi Club"));

        Response response = controller.listUpcoming(0, 20);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("1", response.getHeaderString("X-Total-Count"));
        Object entity = response.getEntity();
        assertTrue("a diferencia del resto de la API, este endpoint embebe objetos completos, no links",
                entity instanceof List);
        @SuppressWarnings("unchecked")
        List<EventDto> events = (List<EventDto>) entity;
        assertEquals(1, events.size());
        assertEquals("Fiesta", events.get(0).getEventName());
        assertEquals("Mi Club", events.get(0).getClubName());
    }
}
