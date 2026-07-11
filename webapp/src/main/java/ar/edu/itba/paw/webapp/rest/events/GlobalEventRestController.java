package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.persistence.EventDao;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    @Autowired
    private EventDao eventDao;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response listUpcoming(@QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("20") int size) {
        if (page < 0 || size <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<Event> events = eventService.listUpcoming(page, size);
        int total = eventService.countUpcoming();
        List<UUID> eventIds = events.stream().map(Event::getEventId).toList();
        Map<UUID, String> clubNames = eventDao.mapClubNamesForEvents(eventIds);

        List<EventDto> body = events.stream()
                .map(event -> eventDtoAssembler.toDto(uriInfo, event, clubNames.get(event.getEventId())))
                .collect(Collectors.toList());

        GenericEntity<List<EventDto>> entity = new GenericEntity<>(body) {};

        Response.ResponseBuilder response = Response.ok(entity)
                .header("X-Total-Count", total);
        addPaginationLinks(response, page, size, total);
        return response.build();
    }

    private void addPaginationLinks(Response.ResponseBuilder responseBuilder, int page, int size, int total) {
        int totalPages = (int) Math.ceil((double) total / size);
        int lastPage = Math.max(0, totalPages - 1);
        List<String> links = new ArrayList<>();
        if (page > 0) {
            links.add(pageLink(0, size, "first"));
            links.add(pageLink(page - 1, size, "prev"));
        }
        if (page < lastPage) {
            links.add(pageLink(page + 1, size, "next"));
            links.add(pageLink(lastPage, size, "last"));
        }
        if (!links.isEmpty()) {
            responseBuilder.header("Link", String.join(", ", links));
        }
    }

    private String pageLink(int page, int size, String rel) {
        return "<" + uriInfo.getRequestUriBuilder()
                .replaceQueryParam("page", page)
                .replaceQueryParam("size", size)
                .build() + ">; rel=\"" + rel + "\"";
    }
}
