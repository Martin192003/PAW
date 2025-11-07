package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;

import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingDao {
    Optional<Booking> findById(UUID id);
    List<Booking> findByUser(UUID userId);
    List<Booking> findByTable(UUID tableId);

    public Booking create(User user, UUID table, Status status, BigDecimal totalAmount, OffsetDateTime bookingStart, OffsetDateTime bookingEnd);

    Optional<Booking> update(UUID bookingId, User user, UUID tableId, Status status, BigDecimal totalAmount, OffsetDateTime bookingStart, OffsetDateTime bookingEnd);

    Optional<Booking> updateStatus(UUID bookingId, Status newStatus);

    boolean delete(UUID bookingId);

    int countAll();

    List<Booking> searchByClubIdsPaginated(List<UUID> clubIds,
                                           UUID bookingId,
                                           Status status,
                                           String sort,
                                           LocalDate bookingDate,
                                           Boolean hasReceipt,
                                           int limit, int offset);

    List<Booking> searchPaginated(UUID userId, UUID clubId, UUID bookingId, Status status, String sort, LocalDate bookingDate, Boolean hasReceipt, int limit, int offset);

    int countSearch(UUID userId, UUID clubId, UUID bookingId, Status status, LocalDate bookingDate, Boolean hasReceipt);

    int countByClubIds(List<UUID> clubIds, UUID bookingId, Status status, LocalDate bookingDate, Boolean hasReceipt);

    boolean existsPastConfirmedByUserAndClub(UUID userId, UUID clubId, OffsetDateTime now);
}
