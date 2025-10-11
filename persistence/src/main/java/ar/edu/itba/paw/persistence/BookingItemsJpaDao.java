package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.BookingItem;
import ar.edu.itba.paw.model.Product;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class BookingItemsJpaDao implements BookingItemsDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public BookingItem create(Booking booking, Product product, int quantity, BigDecimal unitPrice) {
        final Booking managedBooking =
                em.contains(booking) ? booking : em.getReference(Booking.class, booking.getBookingId());
        final Product managedProduct =
                em.contains(product) ? product : em.getReference(Product.class, product.getProductId());

        BookingItem bi = new BookingItem();
        bi.setBookingItemId(UUID.randomUUID());
        bi.setBooking(managedBooking);
        bi.setProduct(managedProduct);
        bi.setQuantity(quantity);
        // si no te pasan precio unitario, tomá el base del producto (ajustá si tu modelo lo requiere)
        bi.setUnitPrice(unitPrice != null ? unitPrice : managedProduct.getBasePrice());
        em.persist(bi);
        return bi;
    }

    @Override
    @Transactional
    public BookingItem update(UUID id, Booking booking, Product product, int quantity, BigDecimal unitPrice) {
        BookingItem bi = em.find(BookingItem.class, id);
        if (bi == null) return null;
        final Booking managedBooking =
                em.contains(booking) ? booking : em.getReference(Booking.class, booking.getBookingId());
        final Product managedProduct =
                em.contains(product) ? product : em.getReference(Product.class, product.getProductId());
        bi.setBooking(managedBooking);
        bi.setProduct(managedProduct);

        bi.setQuantity(quantity);
        bi.setUnitPrice(unitPrice);
        return bi;
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        BookingItem bi = em.find(BookingItem.class, id);
        if (bi == null) return false;
        em.remove(bi);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingItem> findById(UUID id) {
        return Optional.ofNullable(em.find(BookingItem.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingItem> findAll() {
        return em.createQuery("SELECT bi FROM BookingItem bi", BookingItem.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingItem> findByBooking(Booking booking) {
        return em.createQuery("""
                SELECT bi
                FROM BookingItem bi
                WHERE bi.booking = :b
                """, BookingItem.class)
                .setParameter("b", booking)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingItem> findByProduct(Product product) {
        return em.createQuery("""
                SELECT bi
                FROM BookingItem bi
                WHERE bi.product = :p
                """, BookingItem.class)
                .setParameter("p", product)
                .getResultList();
    }
}
