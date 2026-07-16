package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Path("/clubs")
@Produces({ApiMediaTypes.EVENT, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
@Consumes({ApiMediaTypes.EVENT, ApiMediaTypes.APPLICATION_JSON})
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
        if (page < 0 || size <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<Event> events = eventService.listByClub(clubId, page, size);
        int total = eventService.countByClub(clubId);

        List<LinkDto> body = CollectionLinks.items(events, event -> uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/events/{eventId}")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("eventId", event.getEventId())
                .build());

        Response.ResponseBuilder response = Response.ok(new GenericEntity<List<LinkDto>>(body) {})
                .header("X-Total-Count", total);
        addPaginationLinks(response, page, size, total);
        return response.build();
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
                .entity(toDtoWithClubName(clubId, created))
                .build();
    }

    @GET
    @Path("/{clubId}/events/{eventId}")
    public Response getById(@PathParam("clubId") UUID clubId,
                            @PathParam("eventId") UUID eventId) {

        Event event = eventService.getForClub(clubId, eventId);

        return Response.ok(toDtoWithClubName(clubId, event)).build();
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

        return Response.ok(toDtoWithClubName(clubId, updated)).build();
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

    private EventDto toDtoWithClubName(UUID clubId, Event event) {
        String clubName = clubService.getClubById(clubId)
                .map(club -> club.getClubName())
                .orElse(null);
        return eventDtoAssembler.toDto(uriInfo, clubId, event, clubName);
    }

}
