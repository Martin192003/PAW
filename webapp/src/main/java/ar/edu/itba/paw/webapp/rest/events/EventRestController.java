package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Path("/clubs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventRestController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventDtoAssembler eventDtoAssembler;

    @Autowired
    private ClubService clubService;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private UriInfo uriInfo;


    @GET
    @Path("/{clubId}/events")
    public Response listByClub(@PathParam("clubId") UUID clubId,
                               @QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("20") int size) {

        List<Event> events = eventService.listByClub(clubId, page, size);

        List<EventDto> body = events.stream()
                .map(event -> eventDtoAssembler.toDto(uriInfo,clubId, event))
                .collect(Collectors.toList());

        GenericEntity<List<EventDto>> entity = new GenericEntity<>(body) {};

        return Response.ok(entity)
                .header("X-Total-Count", eventService.countByClub(clubId))
                .build();
    }

    @GET
    @Path("/events")
    @Deprecated
    public Response getUpcoming(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("20") int size){
        List<Event> events = eventService.listUpcoming(page, size);
        List<EventDto> body = events.stream()
                .map(event ->eventDtoAssembler.toDto(uriInfo,event))
                .collect(Collectors.toList());

        GenericEntity<List<EventDto>> entity = new GenericEntity<>(body) {};

        return Response.ok(entity)
                .header("X-Total-Count", eventService.countUpcoming())
                .header("Warning", "299 - \"Deprecated endpoint; use /api/events\"")
                .build();
    }

    @POST
    @Path("/{clubId}/events")
    public Response create(@PathParam("clubId") UUID clubId,
                           @Valid EventDto dto,
                           @Context SecurityContext securityContext) {

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubId, user.getId());

        Event created = eventService.create(
                clubId,
                dto.getEventName(),
                dto.getDescription(),
                dto.getDate()
        );

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getEventId().toString())
                .build();

        return Response.created(location)
                .entity(eventDtoAssembler.toDto(uriInfo,clubId, created))
                .build();
    }

    @GET
    @Path("/{clubId}/events/{eventId}")
    public Response getById(@PathParam("clubId") UUID clubId,
                            @PathParam("eventId") UUID eventId) {

        Event event = eventService.getForClub(clubId, eventId);

        return Response.ok( eventDtoAssembler.toDto(uriInfo,clubId, event)).build();
    }

    @PUT
    @Path("/{clubId}/events/{eventId}")
    public Response update(@PathParam("clubId") UUID clubId,
                           @PathParam("eventId") UUID eventId,
                           @Valid EventDto dto,
                           @Context SecurityContext securityContext) {

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubId, user.getId());

        Event updated = eventService.update(
                clubId,
                eventId,
                dto.getEventName(),
                dto.getDescription(),
                dto.getDate(),
                null
        );

        return Response.ok( eventDtoAssembler.toDto(uriInfo,clubId, updated)).build();
    }

    @DELETE
    @Path("/{clubId}/events/{eventId}")
    public Response delete(@PathParam("clubId") UUID clubId,
                           @PathParam("eventId") UUID eventId,
                           @Context SecurityContext securityContext) {

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubId, user.getId());

        eventService.delete(clubId, eventId);
        return Response.noContent().build();
    }

}
