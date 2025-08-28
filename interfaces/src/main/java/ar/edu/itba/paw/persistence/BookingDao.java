package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingDao {
    Booking findById(UUID id);
    List<Booking> findAll();

    List<Booking> findByUser(User user);
    List<Booking> findByTable(UUID tableId); // si luego modelás Table, podés cambiar a Table

    Booking create(User user, UUID tableId, String status, BigDecimal totalAmount,
                   OffsetDateTime bookingStart, OffsetDateTime bookingEnd);

    Booking update(Booking booking);
    boolean delete(UUID bookingId);
}
