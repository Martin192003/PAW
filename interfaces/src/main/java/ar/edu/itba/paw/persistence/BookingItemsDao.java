package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.BookingItem;
import ar.edu.itba.paw.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingItemsDao {
    Optional<BookingItem> findById(UUID id);
    List<BookingItem> findAll();
    List<BookingItem> findByBooking(Booking booking);
    List<BookingItem> findByProduct(Product product);

    BookingItem create(Booking booking, Product product, int quantity, BigDecimal unitPrice);
    BookingItem update(UUID id, Booking booking, Product product, int quantity, BigDecimal unitPrice);
    boolean delete(UUID id);
}
