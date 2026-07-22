package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ValidationGroups;
import ar.edu.itba.paw.model.exceptions.BookingNotFoundException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
import ar.edu.itba.paw.webapp.rest.DtoValidation;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Path("/bookings")
@Produces({ApiMediaTypes.BOOKING, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
public class BookingsRestController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    private final BookingService bookingService;
    private final ClubService clubService;
    private final BookingDtoAssembler bookingDtoAssembler;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BookingsRestController(final BookingService bookingService,
                                  final ClubService clubService,
                                  final BookingDtoAssembler bookingDtoAssembler,
                                  final AuthenticatedUserResolver authenticatedUserResolver) {
        this.bookingService = bookingService;
        this.clubService = clubService;
        this.bookingDtoAssembler = bookingDtoAssembler;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GET
    @Produces({ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
    public Response listBookings(@QueryParam("managed") @DefaultValue("false") boolean managed,
                                 @QueryParam("userId") UUID userId,
                                 @QueryParam("clubId") UUID clubId,
                                 @QueryParam("bookingId") String bookingId,
                                 @QueryParam("status") Status status,
                                 @QueryParam("sort") @DefaultValue("created_desc") String sort,
                                 @QueryParam("bookingDate") String bookingDate,
                                 @QueryParam("hasReceipt") Boolean hasReceipt,
                                 @QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("10") int size,
                                 @Context SecurityContext securityContext) {
        page = Math.max(page, DEFAULT_PAGE);
        size = size < 1 ? DEFAULT_SIZE : size;
        final int offset = page * size;
        final User authUser = authenticatedUser(securityContext);

        if (managed && userId != null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (!managed && userId != null && !authUser.getId().equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        final List<Booking> bookings;
        final int total;
        if (managed) {
            final List<UUID> managedClubIds = managedClubScope(authUser, clubId);
            bookings = bookingService.searchByClubIdsPaginated(
                    managedClubIds, bookingId, status, sort, bookingDate, hasReceipt, size, offset);
            total = bookingService.countByClubIds(
                    managedClubIds, bookingId, status, bookingDate, hasReceipt);
        } else {
            final UUID targetUserId = userId != null ? userId : authUser.getId();
            bookings = bookingService.searchPaginated(
                    targetUserId, clubId, bookingId, status, sort, bookingDate, hasReceipt, size, offset);
            total = bookingService.countSearch(
                    targetUserId, clubId, bookingId, status, bookingDate, hasReceipt);
        }

        final int totalPages = (int) Math.ceil((double) total / size);
        Response.ResponseBuilder response = privateCollectionResponse(bookings, total, managed, !managed);
        addPaginationLinks(response, page, size, totalPages);
        return response.build();
    }

    @GET
    @Path("/{bookingId}")
    public Response getBookingById(@PathParam("bookingId") UUID bookingId,
                                   @QueryParam("managed") @DefaultValue("false") boolean managed,
                                   @Context SecurityContext securityContext) {
        final User user = authenticatedUser(securityContext);
        final Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        final UUID clubId = resolveClubId(booking);

        if (managed) {
            assertCanManage(user, clubId);
            return privateResponse(toDto(booking, true, false)).build();
        }

        final boolean isOwner = booking.getUser() != null
                && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId());
        final boolean canManage = !isOwner && canManage(user, clubId);
        if (!isOwner && !canManage) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }
        return privateResponse(toDto(booking, canManage, isOwner)).build();
    }

    @PUT
    @Path("/{bookingId}")
    public Response updateBookingById(@PathParam("bookingId") UUID bookingId,
                                      BookingDto body,
                                      @Context SecurityContext securityContext) {
        if (!DtoValidation.isValid(body, ValidationGroups.Update.class)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final Status status;
        try {
            status = Status.valueOf(body.getStatus());
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final String login = authenticatedLoginIdentifier(securityContext);
        if (status == Status.CONFIRMADA) {
            bookingService.confirmManagedClubBooking(login, bookingId);
        } else if (status == Status.CANCELADA) {
            bookingService.cancelManagedClubBooking(login, bookingId);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return privateResponse(toDto(bookingService.getManagedClubBooking(login, bookingId), true, false)).build();
    }

    @DELETE
    @Path("/{bookingId}")
    public Response cancelBooking(@PathParam("bookingId") UUID bookingId,
                                  @Context SecurityContext securityContext) {
        bookingService.cancelAccessibleBooking(authenticatedLoginIdentifier(securityContext), bookingId);
        return Response.noContent().build();
    }

    private BookingDto toDto(Booking booking, boolean canManage, boolean isOwner) {
        final UUID clubId = resolveClubId(booking);
        final String clubName = clubService.mapClubNames(List.of(booking)).get(booking.getBookingId());
        return bookingDtoAssembler.toDetailDto(uriInfo, clubId, booking, canManage, isOwner, clubName);
    }

    private UUID resolveClubId(Booking booking) {
        if (booking.getTable() == null || booking.getTable().getClubId() == null) {
            return bookingService.clubId(booking.getBookingId());
        }
        return booking.getTable().getClubId();
    }

    private void assertCanManage(User user, UUID clubId) {
        if (!canManage(user, clubId)) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }
    }

    private boolean canManage(User user, UUID clubId) {
        final List<UUID> managedClubIds = clubService.findManagedClubIds(user.getId());
        return managedClubIds != null && managedClubIds.contains(clubId);
    }

    private String authenticatedLoginIdentifier(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("Authentication required");
        }
        return securityContext.getUserPrincipal().getName();
    }

    private User authenticatedUser(SecurityContext securityContext) {
        final User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            throw new NotAuthorizedException("Authentication required");
        }
        return user;
    }

    private List<UUID> managedClubScope(User user, UUID requestedClubId) {
        final List<UUID> managedClubIds = clubService.findManagedClubIds(user.getId());
        if (managedClubIds == null || managedClubIds.isEmpty()) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }
        if (requestedClubId == null) {
            return managedClubIds;
        }
        if (!managedClubIds.contains(requestedClubId)) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }
        return List.of(requestedClubId);
    }

    private Response.ResponseBuilder privateResponse(Object entity) {
        return Response.ok(entity)
                .header("Cache-Control", "private, no-store")
                .header("Pragma", "no-cache")
                .header("Vary", "Authorization");
    }

    private Response.ResponseBuilder privateCollectionResponse(List<Booking> bookings,
                                                               int total,
                                                               boolean canManage,
                                                               boolean isOwner) {
        final List<LinkDto> body = CollectionLinks.items(bookings, booking -> uriInfo.getBaseUriBuilder()
                .path("bookings/{bookingId}")
                .resolveTemplate("bookingId", booking.getBookingId())
                .build());
        return Response.ok(new GenericEntity<List<LinkDto>>(body) {})
                .header("Cache-Control", "private, no-store")
                .header("Pragma", "no-cache")
                .header("X-Total-Count", total)
                .header("Vary", "Authorization");
    }

    private void addPaginationLinks(Response.ResponseBuilder responseBuilder,
                                    int page,
                                    int size,
                                    int totalPages) {
        final int lastPage = DEFAULT_PAGE + totalPages - 1;
        List<String> links = new ArrayList<>();
        if (page > DEFAULT_PAGE) {
            links.add(pageLink(DEFAULT_PAGE, size, "first"));
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
