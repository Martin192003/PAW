package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Path("/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GlobalEventRestController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventDtoAssembler eventDtoAssembler;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response listUpcoming(@QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("20") int size) {
        List<Event> events = eventService.listUpcoming(page, size);
        List<EventDto> body = events.stream()
                .map(event -> eventDtoAssembler.toDto(uriInfo, event))
                .collect(Collectors.toList());

        GenericEntity<List<EventDto>> entity = new GenericEntity<>(body) {};

        return Response.ok(entity)
                .header("X-Total-Count", eventService.countUpcoming())
                .build();
    }
}
