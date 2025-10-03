package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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

    boolean cancel(UUID bookingId);

    boolean delete(UUID bookingId);

    /** Lista paginada con filtros */
    List<Booking> searchPaginated(UUID userId,
                                  UUID clubId,
                                  UUID bookingId,
                                  Status status,
                                  String sort,
                                  int limit,
                                  int offset);

    /** Total para los mismos filtros (sin limit/offset) */
    int countSearch(UUID userId,
                    UUID clubId,
                    UUID bookingId,
                    Status status);

    /** Listado simple paginado (sin filtros) */
    List<Booking> findAllPaginated(int limit, int offset);
    int countAll();
}
