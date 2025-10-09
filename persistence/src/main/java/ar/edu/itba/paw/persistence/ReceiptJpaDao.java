package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Receipt;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.*;

@Repository
public class ReceiptJpaDao implements ReceiptDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public UUID create(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data) {
        Receipt r = new Receipt();
        r.setBookingId(bookingId);
        r.setUploadedBy(uploadedBy);
        r.setFilename(filename);
        r.setContentType(contentType);
        r.setData(data);
        // url/createdAt/sizeBytes/receiptId se setean en @PrePersist
        em.persist(r);
        return r.getReceiptId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findByBooking(UUID bookingId) {
        return em.createQuery("""
                SELECT r
                FROM Receipt r
                WHERE r.bookingId = :bid
                """, Receipt.class)
                .setParameter("bid", bookingId)
                .setMaxResults(1).getResultList().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findById(UUID receiptId) {
        return Optional.ofNullable(em.find(Receipt.class, receiptId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findByIdWithData(UUID receiptId) {
        // Igual que findById; 'data' es LAZY y se carga al acceder dentro de la transacción
        return Optional.ofNullable(em.find(Receipt.class, receiptId));
    }

    @Override
    @Transactional
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
    @Transactional
    public boolean delete(UUID receiptId) {
        Receipt r = em.find(Receipt.class, receiptId);
        if (r == null) return false;
        em.remove(r);
        return true;
    }
}
