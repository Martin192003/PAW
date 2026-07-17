package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Receipt;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.time.OffsetDateTime;
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
        final List<Object[]> list = em.createQuery("""
                SELECT r.receiptId,
                       r.bookingId,
                       r.uploadedBy,
                       r.url,
                       r.filename,
                       r.contentType,
                       r.sizeBytes,
                       r.createdAt
                FROM Receipt r
                WHERE r.bookingId = :bid
                ORDER BY r.createdAt DESC
                """, Object[].class)
                .setParameter("bid", bookingId)
                .setMaxResults(1)
                .getResultList();

        if (list.isEmpty()) return Optional.empty();

        final Object[] row = list.getFirst();
        return Optional.of(new Receipt(
                (UUID) row[0],
                (UUID) row[1],
                (UUID) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                (Long) row[6],
                (OffsetDateTime) row[7],
                null
        ));
    }

    @Override
    public Set<UUID> findBookingIdsWithReceipt(Collection<UUID> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return Collections.emptySet();
        }

        final List<UUID> rows = em.createQuery("""
                SELECT DISTINCT r.bookingId
                FROM Receipt r
                WHERE r.bookingId IN :bookingIds
                """, UUID.class)
                .setParameter("bookingIds", bookingIds)
                .getResultList();

        return new HashSet<>(rows);
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
