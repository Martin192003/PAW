package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Receipt;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.*;

@Repository
public class ReceiptJpaDao implements ReceiptDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public UUID create(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data) {
        Receipt r = new Receipt();
        r.setBookingId(bookingId);
        r.setUploadedBy(uploadedBy);
        r.setFilename(filename);
        r.setContentType(contentType);
        r.setData(data);
        em.persist(r);
        return r.getReceiptId();
    }

    @Override
    public Optional<Receipt> findByBooking(UUID bookingId) {
        final List<Receipt> list = em.createQuery("""
                SELECT r
                FROM Receipt r
                WHERE r.bookingId = :bid
                ORDER BY r.createdAt DESC
                """, Receipt.class)
                .setParameter("bid", bookingId)
                .setMaxResults(1)
                .getResultList();

        if (list.isEmpty()) return Optional.empty();

        Receipt r = list.getFirst();
        em.detach(r);
        r.setData(null);
        return Optional.of(r);
    }

    @Override
    public Optional<Receipt> findById(UUID receiptId) {
        return Optional.ofNullable(em.find(Receipt.class, receiptId));
    }

    @Override
    public Optional<Receipt> findByIdWithData(UUID receiptId) {
        return Optional.ofNullable(em.find(Receipt.class, receiptId));
    }

    @Override
    public boolean deleteByBooking(UUID bookingId) {
        int updated = em.createQuery("""
                DELETE FROM Receipt r
                WHERE r.bookingId = :bid
                """)
                .setParameter("bid", bookingId)
                .executeUpdate();
        return updated > 0;
    }

    @Override
    public boolean delete(UUID receiptId) {
        Receipt r = em.find(Receipt.class, receiptId);
        if (r == null) return false;
        em.remove(r);
        return true;
    }
}
