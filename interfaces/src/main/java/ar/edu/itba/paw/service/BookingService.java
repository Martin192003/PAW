package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

public interface BookingService {

    Booking create(User user,
                   UUID tableId,
                   OffsetDateTime bookingStart,
                   OffsetDateTime bookingEnd,
                   BigDecimal totalAmount);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findByUser(UUID userId);

    Booking updateStatus(UUID bookingId, Status newStatus);

    Booking update(UUID bookingId,
                   User user,
                   UUID tableId,
                   OffsetDateTime bookingStart,
                   OffsetDateTime bookingEnd,
                   BigDecimal totalAmount,
                   Status status);

    void cancel(UUID bookingId);

    void delete(UUID bookingId);

    void cancelByUser(UUID bookingId, UUID userId);

    Booking createForClub(UUID clubId, UUID tableId, LocalDate date, String loginIdentifier);

    // Deja pasar al dueño de la reserva o a un admin del club: se usa para el
    // comprobante de pago, que el admin necesita ver para poder confirmar o
    // rechazar la reserva (antes solo lo podia ver el dueño, y el admin
    // recibia un 403 al intentar verlo).
    Booking getForUserOrAdminInClub(UUID clubId, UUID bookingId, String loginIdentifier);

    List<Booking> searchForUserPaginated(String loginIdentifier, UUID clubId, String bookingId, Status status, String sort, String bookingDate, Boolean hasReceipt, int limit, int offset);

    int countSearchForUser(String loginIdentifier, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt);

    List<Booking> searchManagedClubsPaginated(String loginIdentifier, UUID clubId, String bookingId, Status status, String sort, String bookingDate, Boolean hasReceipt, int limit, int offset);

    int countSearchManagedClubs(String loginIdentifier, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt);

    Booking getManagedClubBooking(String loginIdentifier, UUID bookingId);

    void confirmManagedClubBooking(String loginIdentifier, UUID bookingId);

    void cancelManagedClubBooking(String loginIdentifier, UUID bookingId);

    void confirm(UUID bookingId);

    Booking getForUser(UUID bookingId, UUID userId);

    Booking getForUser(String loginIdentifier, UUID bookingId);

    void cancelForUser(String loginIdentifier, UUID bookingId);

    Booking getAccessibleBooking(String loginIdentifier, UUID bookingId);

    void cancelAccessibleBooking(String loginIdentifier, UUID bookingId);

    List<Booking> searchForAdminPaginated(UUID adminUserId, UUID clubId, String oldBookingId, Status status, String sort, String oldBookingDate, Boolean hasReceipt,int limit, int offset);

    int countSearch(UUID userId, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt);

    List<Booking> searchPaginated(UUID userId, UUID clubId, String bookingId, Status status, String sort, String bookingDate, Boolean hasReceipt, int limit, int offset);

    int countSearchForAdmin(UUID adminUserId, UUID clubId, String oldBookingId, Status status, String oldBookingDate, Boolean hasReceipt);

    String getBookingDateStart(UUID booking);

    String getBookingDateEnd(UUID booking);

    String getDateStart(UUID bookingId, UUID userId);

    String getDateEnd(UUID bookingId, UUID userId);

    User getBookingUser(UUID bookingId, UUID userId);

    ClubTable getBookingTable(UUID bookingId, UUID userId);

    Club getBookingClub(UUID bookingId, UUID userId);

    UUID parseBookingId(String bookingId);

    LocalDate parseBookingDate(String bookingDate);

    String buildQueryParams(Map<String, String> filters);

    Map<UUID, Date> mapBookingStartDates(List<Booking> bookings);

    int getTotalPages(UUID userId, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt, int size);

    UUID clubId(UUID bookingId);
}
