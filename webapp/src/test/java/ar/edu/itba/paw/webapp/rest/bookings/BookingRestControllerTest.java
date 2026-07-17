package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.exceptions.BadRequestException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.service.ReceiptService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BookingRestControllerTest {

    private static final String LOGIN = "user@example.com";
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();

    private BookingService bookingService;
    private ReceiptService receiptService;
    private BookingRestController controller;
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        bookingService = mock(BookingService.class);
        receiptService = mock(ReceiptService.class);
        controller = new BookingRestController(
                bookingService,
                receiptService,
                new BookingDtoAssembler(receiptService),
                new ReceiptDtoAssembler()
        );

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getAbsolutePathBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/clubs/" + CLUB_ID + "/bookings/" + BOOKING_ID + "/receipt"));
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);

        securityContext = mock(SecurityContext.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(LOGIN);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
    }

    @Test
    public void testCreateBooking_NullBody_ThrowsBadRequest() {
        Assert.assertThrows(BadRequestException.class, () ->
                controller.createBooking(CLUB_ID, null, securityContext));
    }

    @Test
    public void testCreateBooking_Unauthenticated_ThrowsNotAuthorized() {
        SecurityContext anonymous = mock(SecurityContext.class);
        when(anonymous.getUserPrincipal()).thenReturn(null);

        BookingDto body = new BookingDto();
        body.setTableId(UUID.randomUUID());

        Assert.assertThrows(NotAuthorizedException.class, () ->
                controller.createBooking(CLUB_ID, body, anonymous));
    }

    @Test
    public void testCreateBooking_Success_ReturnsCreatedWithLocation() {
        BookingDto body = new BookingDto();
        UUID tableId = UUID.randomUUID();
        body.setTableId(tableId);

        Booking created = new Booking();
        created.setBookingId(BOOKING_ID);
        created.setStatus(ar.edu.itba.paw.model.Status.PENDIENTE);
        when(bookingService.createForClub(CLUB_ID, tableId, body.getDate(), LOGIN)).thenReturn(created);
        when(receiptService.findByBooking(BOOKING_ID)).thenReturn(Optional.empty());

        Response response = controller.createBooking(CLUB_ID, body, securityContext);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getLocation());
        assertEquals(true, response.getLocation().toString().contains(BOOKING_ID.toString()));
        assertEquals(true, response.getEntity() instanceof BookingDto);
    }

    @Test
    public void testGetReceipt_NotFound_ThrowsNotFoundException() {
        when(receiptService.getForBookingInClub(CLUB_ID, BOOKING_ID, LOGIN)).thenReturn(Optional.empty());

        Assert.assertThrows(NotFoundException.class, () ->
                controller.getReceipt(CLUB_ID, BOOKING_ID, securityContext));
    }

    @Test
    public void testGetReceipt_Found_ReturnsPrivateNoStoreImage() {
        Receipt receipt = new Receipt(
                UUID.randomUUID(), BOOKING_ID, UUID.randomUUID(), null,
                "recibo.jpg", "image/jpeg", 3L, OffsetDateTime.now(), new byte[]{1, 2, 3}
        );
        when(receiptService.getForBookingInClub(CLUB_ID, BOOKING_ID, LOGIN)).thenReturn(Optional.of(receipt));

        Response response = controller.getReceipt(CLUB_ID, BOOKING_ID, securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("image/jpeg", response.getMediaType().toString());
        assertNotNull(response.getHeaderString("Content-Disposition"));
        assertEquals(true, response.getHeaderString("Cache-Control").contains("private"));
        assertEquals(true, response.getHeaderString("Cache-Control").contains("no-store"));
    }

    @Test
    public void testDeleteReceipt_NotFound_ThrowsNotFoundException() {
        when(receiptService.deleteByBookingInClub(CLUB_ID, BOOKING_ID, LOGIN)).thenReturn(false);

        Assert.assertThrows(NotFoundException.class, () ->
                controller.deleteReceipt(CLUB_ID, BOOKING_ID, securityContext));
    }

    @Test
    public void testDeleteReceipt_Success_ReturnsNoContent() {
        when(receiptService.deleteByBookingInClub(CLUB_ID, BOOKING_ID, LOGIN)).thenReturn(true);

        Response response = controller.deleteReceipt(CLUB_ID, BOOKING_ID, securityContext);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
}
