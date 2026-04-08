package ar.edu.itba.paw.webapp.rest.bookings;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.exceptions.BadRequestException;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Context;
import java.time.LocalDate;
import java.util.UUID;

@Path("/clubs/{clubId}/reserve")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingRestController {

    private final BookingService bookingService;
    private final UserService userService;

    @Autowired
    public BookingRestController(final BookingService bookingService, final UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    @POST
    public Response reserve(@PathParam("clubId") String clubId,
                           ReserveRequest body,
                            @Context SecurityContext securityContext) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("clubId invalido"))
                    .build();
        }

        final String loginIdentifier = resolveLoginIdentifier(securityContext);
        if (loginIdentifier == null || loginIdentifier.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(error("Debes iniciar sesion para reservar"))
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Body requerido"))
                    .build();
        }

        final String tableId = body.getTableId();
        final String date = body.getDate();
        final boolean fromEvent = Boolean.TRUE.equals(body.getFromEvent());

        if (tableId == null || tableId.isBlank() || date == null || date.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("tableId y date son requeridos"))
                    .build();
        }

        final UUID tableUuid;
        try {
            tableUuid = UUID.fromString(tableId);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("tableId invalido"))
                    .build();
        }

        final LocalDate bookingDate;
        try {
            bookingDate = LocalDate.parse(date);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("date invalida, formato esperado yyyy-MM-dd"))
                    .build();
        }

        final User user = findAuthenticatedUser(loginIdentifier);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(error("Usuario no autenticado"))
                    .build();
        }

        try {
            final Booking booking = bookingService.reserveTableForClub(clubUuid, tableUuid, bookingDate, loginIdentifier, fromEvent);
            return Response.status(Response.Status.CREATED)
                    .entity(new ReserveResponse(booking.getBookingId().toString(), "Reserva creada correctamente"))
                    .build();
        } catch (InvalidDateRangeException | ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error(errorMessageOrDefault(e, "Solicitud invalida")))
                    .build();
        } catch (BadRequestException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(error(errorMessageOrDefault(e, "No se pudo completar la reserva")))
                    .build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(error("No tienes permisos para reservar esta mesa"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error(errorMessageOrDefault(e, "No se pudo completar la reserva")))
                    .build();
        }
    }

    private static ErrorDto error(String text) {
        return new ErrorDto(text);
    }

    private static String errorMessageOrDefault(Exception e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }

    private String resolveLoginIdentifier(SecurityContext securityContext) {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            return securityContext.getUserPrincipal().getName();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        return principal != null ? principal.toString() : null;
    }

    private User findAuthenticatedUser(String loginIdentifier) {
        User user = userService.findByUserName(loginIdentifier);
        if (user == null) {
            user = userService.findByEmail(loginIdentifier);
        }
        return user;
    }

    public static class ReserveRequest {
        private String tableId;
        private String date;
        private Boolean fromEvent;

        public String getTableId() {
            return tableId;
        }

        public void setTableId(String tableId) {
            this.tableId = tableId;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Boolean getFromEvent() {
            return fromEvent;
        }

        public void setFromEvent(Boolean fromEvent) {
            this.fromEvent = fromEvent;
        }
    }

    public static class ReserveResponse {
        private String bookingId;
        private String message;

        public ReserveResponse() {
        }

        public ReserveResponse(String bookingId, String message) {
            this.bookingId = bookingId;
            this.message = message;
        }

        public String getBookingId() {
            return bookingId;
        }

        public String getMessage() {
            return message;
        }
    }
}

