package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Receipt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertThrows;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ReceiptJpaDaoTest {

    @Autowired
    private ReceiptJpaDao receiptDao;

    @PersistenceContext
    private EntityManager em;

    private UUID bookingId;
    private UUID uploadedBy;
    private byte[] data;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Receipt r").executeUpdate();
        em.flush();

        bookingId = UUID.randomUUID();
        uploadedBy = UUID.randomUUID();
        data = new byte[]{1, 2, 3};
    }

    // -------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------

    @Test
    public void testCreate() {
        UUID id = receiptDao.create(
                bookingId,
                uploadedBy,
                "file.jpg",
                "image/jpeg",
                data
        );
        em.flush();

        assertNotNull(id);

        Receipt r = em.find(Receipt.class, id);
        assertNotNull(r);
        assertEquals(bookingId, r.getBookingId());
        assertEquals(uploadedBy, r.getUploadedBy());
        assertEquals("file.jpg", r.getFilename());
        assertEquals("image/jpeg", r.getContentType());
        assertNotNull(r.getUrl());              // generado en prePersist
        assertNotNull(r.getCreatedAt());        // generado en prePersist
        assertEquals(3, r.getSizeBytes().longValue());
        assertArrayEquals(data, r.getData());
    }

    @Test
    public void testCreateNullBookingThrows() {
        assertThrows(PersistenceException.class, () -> {
            receiptDao.create(
                    null,
                    uploadedBy,
                    "file.jpg",
                    "image/jpeg",
                    data
            );
            em.flush();
        });
    }

    @Test
    public void testCreateNullUploadedByThrows() {
        assertThrows(PersistenceException.class, () -> {
            receiptDao.create(
                    bookingId,
                    null,
                    "file.jpg",
                    "image/jpeg",
                    data
            );
            em.flush();
        });
    }

    @Test
    public void testCreateNullDataThrows() {
        assertThrows(PersistenceException.class, () -> {
            receiptDao.create(
                    bookingId,
                    uploadedBy,
                    "file.jpg",
                    "image/jpeg",
                    null
            );
            em.flush();
        });
    }

    @Test
    public void testCreateNullFilenameThrows() {
        assertThrows(PersistenceException.class, () -> {
            receiptDao.create(
                    bookingId,
                    uploadedBy,
                    null,
                    "image/jpeg",
                    data
            );
            em.flush();
        });
    }

    @Test
    public void testCreateNullContentTypeThrows() {
        assertThrows(PersistenceException.class, () -> {
            receiptDao.create(
                    bookingId,
                    uploadedBy,
                    "file.jpg",
                    null,
                    data
            );
            em.flush();
        });
    }

    // -------------------------------------------------------------------
    // FIND BY ID (without data)
    // -------------------------------------------------------------------

    @Test
    public void testFindById() {
        UUID id = insertReceipt();
        em.flush();

        Optional<Receipt> r = receiptDao.findById(id);

        assertTrue(r.isPresent());
        assertEquals(id, r.get().getReceiptId());
        assertEquals("test.jpg", r.get().getFilename());
        assertEquals("image/jpeg", r.get().getContentType());
        assertNotNull(r.get().getData()); // findById NO limpia data
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Receipt> r = receiptDao.findById(UUID.randomUUID());
        assertFalse(r.isPresent());
    }

    // -------------------------------------------------------------------
    // FIND BY ID WITH DATA
    // -------------------------------------------------------------------

    @Test
    public void testFindByIdWithData() {
        UUID id = insertReceipt();
        em.flush();

        Optional<Receipt> r = receiptDao.findByIdWithData(id);

        assertTrue(r.isPresent());
        assertArrayEquals(data, r.get().getData());
    }

    @Test
    public void testFindByIdWithDataNotFound() {
        Optional<Receipt> r = receiptDao.findByIdWithData(UUID.randomUUID());
        assertFalse(r.isPresent());
    }

    // -------------------------------------------------------------------
    // FIND BY BOOKING
    // -------------------------------------------------------------------

    @Test
    public void testFindByBooking() {
        UUID id = insertReceipt();
        em.flush();

        Optional<Receipt> r = receiptDao.findByBooking(bookingId);

        assertTrue(r.isPresent());
        assertEquals(id, r.get().getReceiptId());
        assertNull(r.get().getData()); // findByBooking detacha y pone data = null
    }

    @Test
    public void testFindByBookingReturnsLatest() {
        // Insert older receipt
        insertReceiptWithCreatedAt(OffsetDateTime.now().minusDays(1));

        // Insert latest receipt
        UUID newestId = insertReceiptWithCreatedAt(OffsetDateTime.now());

        em.flush();

        Optional<Receipt> r = receiptDao.findByBooking(bookingId);

        assertTrue(r.isPresent());
        assertEquals(newestId, r.get().getReceiptId());
    }

    @Test
    public void testFindByBookingNotFound() {
        Optional<Receipt> r = receiptDao.findByBooking(UUID.randomUUID());
        assertFalse(r.isPresent());
    }

    // -------------------------------------------------------------------
    // DELETE BY BOOKING
    // -------------------------------------------------------------------

    @Test
    public void testDeleteByBooking() {
        insertReceipt();
        em.flush();

        boolean deleted = receiptDao.deleteByBooking(bookingId);
        em.flush();

        assertTrue(deleted);
        assertEquals(0L, countReceipts());
    }

    @Test
    public void testDeleteByBookingNotFound() {
        boolean deleted = receiptDao.deleteByBooking(UUID.randomUUID());
        assertFalse(deleted);
    }

    // -------------------------------------------------------------------
    // DELETE BY ID
    // -------------------------------------------------------------------

    @Test
    public void testDelete() {
        UUID id = insertReceipt();
        em.flush();

        boolean deleted = receiptDao.delete(id);
        em.flush();

        assertTrue(deleted);
        assertEquals(0L, countReceipts());
    }

    @Test
    public void testDeleteNotFound() {
        boolean deleted = receiptDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private UUID insertReceipt() {
        return insertReceiptWithCreatedAt(OffsetDateTime.now());
    }

    private UUID insertReceiptWithCreatedAt(OffsetDateTime createdAt) {
        UUID id = UUID.randomUUID();

        em.createNativeQuery("""
                INSERT INTO receipts (receipt_id, booking_id, uploaded_by, url, filename,
                                       content_type, size_bytes, created_at, data)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
                .setParameter(1, id)
                .setParameter(2, bookingId)
                .setParameter(3, uploadedBy)
                .setParameter(4, "http://test.com/receipt")
                .setParameter(5, "test.jpg")
                .setParameter(6, "image/jpeg")
                .setParameter(7, (long) data.length)
                .setParameter(8, createdAt)
                .setParameter(9, data)
                .executeUpdate();

        return id;
    }

    private long countReceipts() {
        return em.createQuery("SELECT COUNT(r) FROM Receipt r", Long.class)
                .getSingleResult();
    }
}
