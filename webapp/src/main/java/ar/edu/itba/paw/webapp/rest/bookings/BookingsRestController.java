package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
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
import java.util.UUID;
import java.util.ArrayList;

@Path("/")
@Produces({ApiMediaTypes.BOOKING, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
public class BookingsRestController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private final BookingService bookingService;
    private final BookingDtoAssembler bookingDtoAssembler;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BookingsRestController(final BookingService bookingService,
                                  final BookingDtoAssembler bookingDtoAssembler,
                                  final AuthenticatedUserResolver authenticatedUserResolver) {
        this.bookingService = bookingService;
        this.bookingDtoAssembler = bookingDtoAssembler;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GET
    @Path("/bookings")
    public Response listBookings(@QueryParam("managed") @DefaultValue("false") boolean managed,
                                 @QueryParam("clubId") UUID clubId,
                                 @QueryParam("bookingId") String bookingId,
                                 @QueryParam("status") Status status,
                                 @QueryParam("sort") @DefaultValue("created_desc") String sort,
                                 @QueryParam("bookingDate") String bookingDate,
                                 @QueryParam("hasReceipt") Boolean hasReceipt,
                                 @QueryParam("page") @DefaultValue("1") int page,
                                 @QueryParam("size") @DefaultValue("10") int size,
                                 @Context SecurityContext securityContext) {
        page = Math.max(page, DEFAULT_PAGE);
        size = size < 1 ? DEFAULT_SIZE : size;
        final int offset = (page - 1) * size;
        final String login = authenticatedLoginIdentifier(securityContext);

        final List<Booking> bookings = managed
                ? bookingService.searchManagedClubsPaginated(
                        login, clubId, bookingId, status, sort, bookingDate, hasReceipt, size, offset)
                : bookingService.searchForUserPaginated(
                        login, clubId, bookingId, status, sort, bookingDate, hasReceipt, size, offset);
        final int total = managed
                ? bookingService.countSearchManagedClubs(
                        login, clubId, bookingId, status, bookingDate, hasReceipt)
                : bookingService.countSearchForUser(
                        login, clubId, bookingId, status, bookingDate, hasReceipt);

        final int totalPages = (int) Math.ceil((double) total / size);
        Response.ResponseBuilder response = privateCollectionResponse(bookings, total);
        addPaginationLinks(response, page, size, totalPages);
        return response.build();
    }

    @GET
    @Path("/clubs/{clubId}/bookings")
    public Response listManagedClubBookings(@PathParam("clubId") UUID clubId,
                                            @QueryParam("bookingId") String bookingId,
                                            @QueryParam("status") Status status,
                                            @QueryParam("sort") @DefaultValue("created_desc") String sort,
                                            @QueryParam("bookingDate") String bookingDate,
                                            @QueryParam("hasReceipt") Boolean hasReceipt,
                                            @QueryParam("page") @DefaultValue("1") int page,
                                            @QueryParam("size") @DefaultValue("10") int size,
                                            @Context SecurityContext securityContext) {
        return listBookings(true, clubId, bookingId, status, sort, bookingDate, hasReceipt, page, size, securityContext);
    }

    @GET
    @Path("/bookings/{bookingId}")
    public Response getBookingById(@PathParam("bookingId") UUID bookingId,
                                   @QueryParam("managed") @DefaultValue("false") boolean managed,
                                   @Context SecurityContext securityContext) {
        final String login = authenticatedLoginIdentifier(securityContext);
        if (managed) {
            final Booking booking = bookingService.getManagedClubBooking(login, bookingId);
            return privateResponse(toDto(booking, true, false)).build();
        }

        final User user = authenticatedUser(securityContext);
        final Booking booking = bookingService.getAccessibleBooking(login, bookingId);
        final boolean isOwner = booking.getUser() != null
                && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId());
        final boolean canManage = canManageBooking(login, bookingId);
        return privateResponse(toDto(booking, canManage, isOwner)).build();
    }

    @PUT
    @Path("/bookings/{bookingId}")
    public Response updateBookingById(@PathParam("bookingId") UUID bookingId,
                                      BookingDto body,
                                      @Context SecurityContext securityContext) {
        if (body == null || body.getStatus() == null || body.getStatus().isBlank()) {
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
    @Path("/bookings/{bookingId}")
    public Response cancelBooking(@PathParam("bookingId") UUID bookingId,
                                  @Context SecurityContext securityContext) {
        bookingService.cancelAccessibleBooking(authenticatedLoginIdentifier(securityContext), bookingId);
        return Response.noContent().build();
    }

    private BookingDto toDto(Booking booking, boolean canManage, boolean isOwner) {
        final UUID clubId = bookingService.clubId(booking.getBookingId());
        return bookingDtoAssembler.toDto(uriInfo, clubId, booking, canManage, isOwner);
    }

    private boolean canManageBooking(String loginIdentifier, UUID bookingId) {
        try {
            bookingService.getManagedClubBooking(loginIdentifier, bookingId);
            return true;
        } catch (UnauthorizedException ignored) {
            return false;
        }
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

    private Response.ResponseBuilder privateResponse(Object entity) {
        return Response.ok(entity)
                .header("Cache-Control", "private, no-store")
                .header("Pragma", "no-cache")
                .header("Vary", "Authorization");
    }

    private Response.ResponseBuilder privateCollectionResponse(List<Booking> bookings, int total) {
        List<LinkDto> links = CollectionLinks.items(bookings, booking -> uriInfo.getBaseUriBuilder()
                .path("bookings/{bookingId}")
                .resolveTemplate("bookingId", booking.getBookingId())
                .build());
        return Response.ok(new GenericEntity<List<LinkDto>>(links) {})
                .header("Cache-Control", "private, no-store")
                .header("Pragma", "no-cache")
                .header("X-Total-Count", total)
                .header("Vary", "Authorization");
    }

    private void addPaginationLinks(Response.ResponseBuilder responseBuilder,
                                    int page,
                                    int size,
                                    int totalPages) {
        List<String> links = new ArrayList<>();
        if (page > DEFAULT_PAGE) {
            links.add(pageLink(DEFAULT_PAGE, size, "first"));
            links.add(pageLink(page - 1, size, "prev"));
        }
        if (page < totalPages) {
            links.add(pageLink(page + 1, size, "next"));
            links.add(pageLink(totalPages, size, "last"));
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
