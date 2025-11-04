package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingService {

    Booking create(User user,
                   UUID tableId,
                   OffsetDateTime bookingStart,
                   OffsetDateTime bookingEnd,
                   BigDecimal totalAmount);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findByUser(UUID userId);

    List<Booking> findByTable(UUID tableId);

    Booking updateStatus(UUID bookingId, Status newStatus);

    Booking update(UUID bookingId,
                   User user,
                   UUID tableId,
                   OffsetDateTime bookingStart,
                   OffsetDateTime bookingEnd,
                   BigDecimal totalAmount,
                   Status status);

    /** Comando: lanza excepción si no existe o no es cancelable. */
    void cancel(UUID bookingId);

    /** Comando: lanza excepción si no existe. */
    void delete(UUID bookingId);

    /** Comando: lanza excepción si usuario no es dueño. */
    void cancelByUser(UUID bookingId, UUID userId);

    /** Devuelve la reserva creada (mejor que void). */
    Booking reserveTableForClub(UUID clubId, UUID tableId, LocalDate date, String username);

    /** Comando idempotente: confirma si está PENDIENTE. */
    void confirm(UUID bookingId);

    /** Acceso controlado a una reserva por usuario. */
    Booking getForUser(UUID bookingId, UUID userId);

    /** Vista admin con paginado. */
    List<Booking> searchForAdminPaginated(UUID adminUserId, UUID clubId, UUID bookingId, Status status, String sort, LocalDate bookingDate, Boolean hasReceipt,int limit, int offset);

    int countSearch(UUID userId, UUID clubId, UUID bookingId, Status status, LocalDate bookingDate, Boolean hasReceipt);

    List<Booking> searchPaginated(UUID userId, UUID clubId, UUID bookingId, Status status, String sort, LocalDate bookingDate, Boolean hasReceipt, int limit, int offset);

    int countSearchForAdmin(UUID adminUserId, UUID clubId, UUID bookingId, Status status, LocalDate bookingDate, Boolean hasReceipt);

}
