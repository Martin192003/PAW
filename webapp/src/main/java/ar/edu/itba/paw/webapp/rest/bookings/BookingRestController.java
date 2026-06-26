package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.model.dto.BookingCreateRequestDto;
import ar.edu.itba.paw.model.exceptions.BadRequestException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.service.ReceiptService;
import ar.edu.itba.paw.webapp.rest.ContentDispositionHeader;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Path("/clubs/{clubId}/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingRestController {

    private final BookingService bookingService;
    private final ReceiptService receiptService;
    private final BookingDtoAssembler bookingDtoAssembler;
    private final ReceiptDtoAssembler receiptDtoAssembler;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    @Autowired
    public BookingRestController(final BookingService bookingService,
                                 final ReceiptService receiptService,
                                 final BookingDtoAssembler bookingDtoAssembler,
                                 final ReceiptDtoAssembler receiptDtoAssembler) {
        this.bookingService = bookingService;
        this.receiptService = receiptService;
        this.bookingDtoAssembler = bookingDtoAssembler;
        this.receiptDtoAssembler = receiptDtoAssembler;
    }

    @POST
    public Response createBooking(@PathParam("clubId") UUID clubId,
                                  BookingCreateRequestDto body,
                                  @Context SecurityContext securityContext) {
        if (body == null) {
            throw new BadRequestException("Body requerido");
        }

        final Booking booking = bookingService.createForClub(
                clubId,
                body.getTableId(),
                body.getDate(),
                authenticatedLoginIdentifier(securityContext)
        );

        final URI location = uriInfo.getAbsolutePathBuilder()
                .path(booking.getBookingId().toString())
                .build();

        return Response.created(location)
                .entity(bookingDtoAssembler.toDto(uriInfo, clubId, booking))
                .build();
    }

    @GET
    @Path("/{bookingId}")
    public Response getBooking(@PathParam("clubId") UUID clubId,
                               @PathParam("bookingId") UUID bookingId,
                               @Context SecurityContext securityContext) {
        final Booking booking = bookingService.getForUserInClub(
                clubId,
                bookingId,
                authenticatedLoginIdentifier(securityContext)
        );

        return Response.ok(bookingDtoAssembler.toDto(uriInfo, clubId, booking)).build();
    }

    @DELETE
    @Path("/{bookingId}")
    public Response cancelBooking(@PathParam("clubId") UUID clubId,
                                  @PathParam("bookingId") UUID bookingId,
                                  @Context SecurityContext securityContext) {
        bookingService.cancelForUserInClub(
                clubId,
                bookingId,
                authenticatedLoginIdentifier(securityContext)
        );

        return Response.noContent().build();
    }

    @PUT
    @Path("/{bookingId}/receipt")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response putReceipt(@PathParam("clubId") UUID clubId,
                               @PathParam("bookingId") UUID bookingId,
                               @Context SecurityContext securityContext) {
        final String loginIdentifier = authenticatedLoginIdentifier(securityContext);
        final boolean existed = receiptService.getForBookingInClub(clubId, bookingId, loginIdentifier).isPresent();
        final Part filePart = filePart();

        final Receipt receipt = receiptService.putForBookingInClub(
                clubId,
                bookingId,
                loginIdentifier,
                filename(filePart),
                filePart.getContentType(),
                bytes(filePart)
        );

        final URI location = uriInfo.getAbsolutePathBuilder().build();
        final Response.ResponseBuilder response = existed ? Response.ok() : Response.created(location);

        return response
                .entity(receiptDtoAssembler.toDto(uriInfo, clubId, receipt))
                .build();
    }

    @GET
    @Path("/{bookingId}/receipt")
    @Produces({"image/jpeg", "image/png", "image/webp"})
    public Response getReceipt(@PathParam("clubId") UUID clubId,
                               @PathParam("bookingId") UUID bookingId,
                               @Context SecurityContext securityContext) {
        final Receipt receipt = receiptService.getForBookingInClub(
                        clubId,
                        bookingId,
                        authenticatedLoginIdentifier(securityContext)
                )
                .orElseThrow(() -> new NotFoundException("receipt.notfound"));

        // Los receipts son documentos privados; nunca deben cachearse en proxies ni CDN.
        CacheControl cacheControl = new CacheControl();
        cacheControl.setPrivate(true);
        cacheControl.setNoStore(true);

        return Response.ok(receipt.getData())
                .type(receipt.getContentType())
                .cacheControl(cacheControl)
                .header("Content-Disposition", ContentDispositionHeader.inline(receipt.getFilename(), "receipt"))
                .build();
    }

    @DELETE
    @Path("/{bookingId}/receipt")
    public Response deleteReceipt(@PathParam("clubId") UUID clubId,
                                  @PathParam("bookingId") UUID bookingId,
                                  @Context SecurityContext securityContext) {
        final boolean deleted = receiptService.deleteByBookingInClub(
                clubId,
                bookingId,
                authenticatedLoginIdentifier(securityContext)
        );

        if (!deleted) {
            throw new NotFoundException("receipt.notfound");
        }
        return Response.noContent().build();
    }

    private String authenticatedLoginIdentifier(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw new NotAuthorizedException("Authentication required");
        }
        return securityContext.getUserPrincipal().getName();
    }

    private Part filePart() {
        try {
            Part filePart = request.getPart("file");
            if (filePart == null) {
                throw new BadRequestException("Missing multipart part 'file'");
            }
            return filePart;
        } catch (IOException | ServletException e) {
            throw new BadRequestException("Invalid multipart request", e);
        }
    }

    private byte[] bytes(Part part) {
        try {
            return part.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new BadRequestException("Unable to read uploaded file", e);
        }
    }

    private String filename(Part part) {
        String filename = part.getSubmittedFileName();
        return filename == null || filename.isBlank() ? "receipt" : filename;
    }

}
