package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ValidationGroups;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
import ar.edu.itba.paw.webapp.rest.DtoValidation;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Path("/events")
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
    @Produces({ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
    public Response list(@QueryParam("clubId") UUID clubId,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        if (page < 0 || size <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        List<Event> events = clubId == null
                ? eventService.listUpcoming(page, size)
                : eventService.listByClub(clubId, page, size);
        int total = clubId == null
                ? eventService.countUpcoming()
                : eventService.countByClub(clubId);

        List<LinkDto> body = CollectionLinks.items(events, event -> uriInfo.getBaseUriBuilder()
                .path("events/{eventId}")
                .resolveTemplate("eventId", event.getEventId())
                .build());

        Response.ResponseBuilder response = Response.ok(new GenericEntity<List<LinkDto>>(body) {})
                .header("X-Total-Count", total)
                .header("Cache-Control", "no-cache");
        addPaginationLinks(response, page, size, total);
        return response.build();
    }

    @POST
    public Response create(@QueryParam("clubId") UUID clubId,
                           EventDto dto,
                           @Context SecurityContext securityContext) {

        if (clubId == null || !DtoValidation.isValid(dto, ValidationGroups.Create.class)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("fields.required"))
                    .build();
        }

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

        URI location = uriInfo.getBaseUriBuilder()
                .path("events/{eventId}")
                .resolveTemplate("eventId", created.getEventId())
                .build();

        return Response.created(location)
                .entity(toDtoWithClubName(created))
                .build();
    }

    @GET
    @Path("/{eventId}")
    public Response getById(@PathParam("eventId") UUID eventId) {

        Event event = eventService.get(eventId);

        return Response.ok(toDtoWithClubName(event)).build();
    }

    @PUT
    @Path("/{eventId}")
    public Response update(@PathParam("eventId") UUID eventId,
                           EventDto dto,
                           @Context SecurityContext securityContext) {

        if (!DtoValidation.isValid(dto, ValidationGroups.Update.class)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("fields.required"))
                    .build();
        }

        Event existing = eventService.get(eventId);
        UUID clubId = existing.getClubId();
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

        return Response.ok(toDtoWithClubName(updated)).build();
    }

    @DELETE
    @Path("/{eventId}")
    public Response delete(@PathParam("eventId") UUID eventId,
                           @Context SecurityContext securityContext) {

        Event existing = eventService.get(eventId);
        UUID clubId = existing.getClubId();
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

    private EventDto toDtoWithClubName(Event event) {
        UUID clubId = event.getClubId();
        String clubName = clubService.getClubById(clubId)
                .map(club -> club.getClubName())
                .orElse(null);
        return eventDtoAssembler.toDto(uriInfo, clubId, event, clubName);
    }

}
