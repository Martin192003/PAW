package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.service.ReceiptService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BookingsRestController es el otro controller central del rediseño de paginación
 * (colecciones "solo-links" + unificación de /managed-bookings en
 * GET /bookings?managed=true, ver AUDITORIA_ENTREGA_FINAL.md puntos 11/12/24) y no
 * tenía ningún test.
 */
public class BookingsRestControllerTest {

    private static final String LOGIN = "user@example.com";
    private static final UUID BOOKING_ID = UUID.randomUUID();

    private BookingService bookingService;
    private BookingDtoAssembler bookingDtoAssembler;
    private ReceiptService receiptService;
    private AuthenticatedUserResolver authenticatedUserResolver;
    private BookingsRestController controller;
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        bookingService = mock(BookingService.class);
        receiptService = mock(ReceiptService.class);
        bookingDtoAssembler = new BookingDtoAssembler(receiptService);
        authenticatedUserResolver = mock(AuthenticatedUserResolver.class);

        controller = new BookingsRestController(
                bookingService,
                bookingDtoAssembler,
                authenticatedUserResolver
        );

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getRequestUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/bookings"));
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);

        securityContext = mock(SecurityContext.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(LOGIN);
        when(securityContext.getUserPrincipal()).thenReturn(principal);

        // getBookingById llama a authenticatedUser(), que exige que el resolver
        // devuelva un User no-nulo (si no, tira NotAuthorizedException). Con esto
        // alcanza para los tests que no les importa quién es el caller exacto.
        when(authenticatedUserResolver.resolve(securityContext)).thenReturn(user(UUID.randomUUID()));
    }

    @Test
    public void testListBookings_UnmanagedUsesSearchForUser() {
        when(bookingService.searchForUserPaginated(
                eq(LOGIN), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(booking(BOOKING_ID, Status.PENDIENTE)));
        when(bookingService.countSearchForUser(eq(LOGIN), any(), any(), any(), any(), any())).thenReturn(3);

        Response response = controller.listBookings(
                false, null, null, null, "created_desc", null, null, 1, 10, securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("3", response.getHeaderString("X-Total-Count"));
        assertEquals("private, no-store", response.getHeaderString("Cache-Control"));
        assertEquals("Authorization", response.getHeaderString("Vary"));

        Object entity = response.getEntity();
        assertTrue(entity instanceof List);
        @SuppressWarnings("unchecked")
        List<LinkDto> links = (List<LinkDto>) entity;
        assertEquals(1, links.size());
        assertTrue(links.get(0).getHref().contains(BOOKING_ID.toString()));
    }

    @Test
    public void testListBookings_ManagedUsesSearchManagedClubs() {
        when(bookingService.searchManagedClubsPaginated(
                eq(LOGIN), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(booking(BOOKING_ID, Status.PENDIENTE)));
        when(bookingService.countSearchManagedClubs(eq(LOGIN), any(), any(), any(), any(), any())).thenReturn(7);

        Response response = controller.listBookings(
                true, null, null, null, "created_desc", null, null, 1, 10, securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Si managed=true no delegara en searchManagedClubsPaginated/countSearchManagedClubs,
        // el total vendría del stub (sin configurar) de la búsqueda "no managed", que
        // devolvería 0 por default -- así que este total ya confirma la rama correcta.
        assertEquals("7", response.getHeaderString("X-Total-Count"));
    }

    @Test
    public void testListManagedClubBookings_DelegatesWithManagedTrue() {
        UUID clubId = UUID.randomUUID();
        when(bookingService.searchManagedClubsPaginated(
                eq(LOGIN), eq(clubId), any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(List.of(booking(BOOKING_ID, Status.PENDIENTE)));
        when(bookingService.countSearchManagedClubs(eq(LOGIN), eq(clubId), any(), any(), any(), any())).thenReturn(2);

        Response response = controller.listManagedClubBookings(
                clubId, null, null, "created_desc", null, null, 1, 10, securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("2", response.getHeaderString("X-Total-Count"));
    }

    @Test
    public void testGetBookingById_NotFound_PropagatesException() {
        when(bookingService.getAccessibleBooking(eq(LOGIN), eq(BOOKING_ID)))
                .thenThrow(new ar.edu.itba.paw.model.exceptions.BookingNotFoundException("booking.notfound"));

        org.junit.Assert.assertThrows(
                ar.edu.itba.paw.model.exceptions.BookingNotFoundException.class,
                () -> controller.getBookingById(BOOKING_ID, securityContext));
    }

    @Test
    public void testGetBookingById_Found_ReturnsBookingDto() {
        User user = user(UUID.randomUUID());
        Booking booking = booking(BOOKING_ID, Status.PENDIENTE);
        booking.setUser(user);
        when(bookingService.getAccessibleBooking(eq(LOGIN), eq(BOOKING_ID))).thenReturn(booking);
        when(bookingService.clubId(BOOKING_ID)).thenReturn(UUID.randomUUID());
        when(bookingService.getManagedClubBooking(eq(LOGIN), eq(BOOKING_ID)))
                .thenThrow(new ar.edu.itba.paw.model.exceptions.UnauthorizedException());
        when(receiptService.findByBooking(BOOKING_ID)).thenReturn(Optional.empty());

        Response response = controller.getBookingById(BOOKING_ID, securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof BookingDto);
        assertEquals(BOOKING_ID, ((BookingDto) response.getEntity()).getId());
        assertEquals("private, no-store", response.getHeaderString("Cache-Control"));
    }

    @Test
    public void testUpdateBookingById_MissingStatus_ReturnsBadRequest() {
        BookingDto body = new BookingDto();

        Response response = controller.updateBookingById(BOOKING_ID, body, securityContext);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateBookingById_InvalidStatus_ReturnsBadRequest() {
        BookingDto body = new BookingDto();
        body.setStatus("NOT_A_REAL_STATUS");

        Response response = controller.updateBookingById(BOOKING_ID, body, securityContext);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateBookingById_Confirm_ReturnsUpdatedBooking() {
        BookingDto body = new BookingDto();
        body.setStatus("CONFIRMADA");

        Booking updated = booking(BOOKING_ID, Status.CONFIRMADA);
        updated.setUser(user(UUID.randomUUID()));
        when(bookingService.getManagedClubBooking(eq(LOGIN), eq(BOOKING_ID))).thenReturn(updated);
        when(bookingService.clubId(BOOKING_ID)).thenReturn(UUID.randomUUID());
        when(receiptService.findByBooking(BOOKING_ID)).thenReturn(Optional.empty());

        Response response = controller.updateBookingById(BOOKING_ID, body, securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof BookingDto);
        assertEquals("CONFIRMADA", ((BookingDto) response.getEntity()).getStatus());
    }

    @Test
    public void testCancelBooking_ReturnsNoContent() {
        Response response = controller.cancelBooking(BOOKING_ID, securityContext);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    private static Booking booking(UUID id, Status status) {
        Booking booking = new Booking();
        booking.setBookingId(id);
        booking.setStatus(status);
        booking.setBookingStart(OffsetDateTime.now());
        booking.setBookingEnd(OffsetDateTime.now().plusHours(6));
        booking.setCreatedAt(OffsetDateTime.now());
        return booking;
    }

    private static User user(UUID id) {
        User user = new User(
                id, "user-" + id, "Name", "Surname", id + "@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455", ar.edu.itba.paw.model.Role.USER, OffsetDateTime.now(), "es"
        );
        user.setVerified(true);
        return user;
    }
}
