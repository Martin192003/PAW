package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingDao {
    Optional<Booking> findById(UUID id);
    List<Booking> findAll();

    List<Booking> findByUser(UUID userId);
    List<Booking> findByTable(UUID tableId);

    public Booking create(User user, UUID tableId, Status status, BigDecimal totalAmount, OffsetDateTime bookingStart, OffsetDateTime bookingEnd);

    Booking update(UUID bookingId, User user, UUID tableId, Status status, BigDecimal totalAmount, OffsetDateTime bookingStart, OffsetDateTime bookingEnd);

    boolean delete(UUID bookingId);

    int countAll();

    List<Booking> searchPaginated(UUID userId,
                                  UUID clubId,
                                  UUID bookingId,
                                  Status status,
                                  String sort,       // created_desc | created_asc | start_desc | start_asc | status_asc
                                  int limit,
                                  int offset);

    int countSearch(UUID userId,
                    UUID clubId,
                    UUID bookingId,
                    Status status);

    List<Booking> findAllPaginated(int limit, int offset);


}
