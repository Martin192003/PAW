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

    Booking reserveTableForClub(UUID clubId, UUID tableId, LocalDate date, String username, boolean fromEvent);

    void confirm(UUID bookingId);

    Booking getForUser(UUID bookingId, UUID userId);

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
