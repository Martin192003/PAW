package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.BookingPageDto;
import ar.edu.itba.paw.service.BookingService;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class BookingsRestController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private final BookingService bookingService;
    private final BookingDtoAssembler bookingDtoAssembler;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public BookingsRestController(final BookingService bookingService,
                                  final BookingDtoAssembler bookingDtoAssembler) {
        this.bookingService = bookingService;
        this.bookingDtoAssembler = bookingDtoAssembler;
    }

    @GET
    @Path("/bookings")
    public Response listUserBookings(@QueryParam("clubId") UUID clubId,
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

        final List<Booking> bookings = bookingService.searchForUserPaginated(
                login, clubId, bookingId, status, sort, bookingDate, hasReceipt, size, offset);
        final int total = bookingService.countSearchForUser(
                login, clubId, bookingId, status, bookingDate, hasReceipt);

        final BookingPageDto pageDto = pageDto(bookings, page, size, total);
        Response.ResponseBuilder response = Response.ok(pageDto);
        addPaginationLinks(response, page, size, pageDto.getTotalPages());
        return response.build();
    }

    @GET
    @Path("/managed-club-bookings")
    public Response listManagedClubBookings(@QueryParam("clubId") UUID clubId,
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

        final List<Booking> bookings = bookingService.searchManagedClubsPaginated(
                login, clubId, bookingId, status, sort, bookingDate, hasReceipt, size, offset);
        final int total = bookingService.countSearchManagedClubs(
                login, clubId, bookingId, status, bookingDate, hasReceipt);

        final BookingPageDto pageDto = pageDto(bookings, page, size, total);
        Response.ResponseBuilder response = Response.ok(pageDto);
        addPaginationLinks(response, page, size, pageDto.getTotalPages());
        return response.build();
    }

    @GET
    @Path("/managed-club-bookings/{bookingId}")
    public Response getManagedClubBooking(@PathParam("bookingId") UUID bookingId,
                                          @Context SecurityContext securityContext) {
        final Booking booking = bookingService.getManagedClubBooking(
                authenticatedLoginIdentifier(securityContext),
                bookingId
        );

        return Response.ok(toDto(booking)).build();
    }

    @PUT
    @Path("/managed-club-bookings/{bookingId}/confirmation")
    @Deprecated
    public Response confirmManagedClubBooking(@PathParam("bookingId") UUID bookingId,
                                              @Context SecurityContext securityContext) {
        bookingService.confirmManagedClubBooking(authenticatedLoginIdentifier(securityContext), bookingId);
        return Response.noContent()
                .header("Warning", "299 - \"Deprecated endpoint; use PUT /api/managed-club-bookings/{bookingId}\"")
                .build();
    }

    @PUT
    @Path("/managed-club-bookings/{bookingId}")
    public Response updateManagedClubBooking(@PathParam("bookingId") UUID bookingId,
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

        return Response.ok(toDto(bookingService.getManagedClubBooking(login, bookingId))).build();
    }

    @DELETE
    @Path("/managed-club-bookings/{bookingId}")
    public Response cancelManagedClubBooking(@PathParam("bookingId") UUID bookingId,
                                             @Context SecurityContext securityContext) {
        bookingService.cancelManagedClubBooking(authenticatedLoginIdentifier(securityContext), bookingId);
        return Response.noContent().build();
    }

    private BookingPageDto pageDto(List<Booking> bookings, int page, int size, int total) {
        final List<BookingDto> dtos = bookings.stream()
                .map(this::toDto)
                .toList();
        final int totalPages = (int) Math.ceil((double) total / size);
        return new BookingPageDto(dtos, page, size, total, totalPages);
    }

    private BookingDto toDto(Booking booking) {
        final UUID clubId = bookingService.clubId(booking.getBookingId());
        return bookingDtoAssembler.toDto(uriInfo, clubId, booking);
    }

    private String authenticatedLoginIdentifier(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("Authentication required");
        }
        return securityContext.getUserPrincipal().getName();
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
