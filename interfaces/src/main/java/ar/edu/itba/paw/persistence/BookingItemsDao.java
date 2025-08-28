package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.BookingItem;
import ar.edu.itba.paw.model.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BookingItemsDao {
    BookingItem findById(UUID id);
    List<BookingItem> findAll();
    List<BookingItem> findByBooking(Booking booking);
    List<BookingItem> findByProduct(Product product);

    BookingItem create(Booking booking, Product product, int quantity, BigDecimal unitPrice, BigDecimal lineTotal);
    BookingItem update(BookingItem bookingItem);
    boolean delete(UUID id);
}
