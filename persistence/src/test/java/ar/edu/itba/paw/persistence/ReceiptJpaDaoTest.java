package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Receipt;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
@Transactional
public class ReceiptJpaDaoTest {

    @Autowired
    private DataSource ds;
    @Autowired
    private ReceiptJpaDao receiptDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "receipts");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "bookings");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "club_tables");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    private void insertUser(UUID userId) {
        String email = userId.toString() + "@example.com";
        jdbcTemplate.update(
            "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            userId, "Test", "User", email, "hash", "123456", "user", OffsetDateTime.now()
        );
    }

    private void insertClub(UUID clubId, UUID contactId) {
        jdbcTemplate.update(
            "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre, zone, is_bar, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            clubId, "Club Test", "Location", 5.0, 1, "medio", "desc", contactId, "pop", "ZONA_CABA", false, OffsetDateTime.now()
        );
    }

    private void insertTable(UUID tableId, UUID clubId) {
        jdbcTemplate.update(
            "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
            tableId, clubId, 1, 4, "disponible", 100.0, 0.0
        );
    }

    private void insertBooking(UUID bookingId, UUID userId, UUID tableId) {
        jdbcTemplate.update(
            "INSERT INTO bookings (booking_id, user_id, table_id, booking_start, booking_end, status, created_at, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            bookingId, userId, tableId, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "confirmada", OffsetDateTime.now(), 100.0
        );
    }

    @Test
    public void testCreateAndFindByIdWithData() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID clubId = UUID.randomUUID();
        insertClub(clubId, userId);
        UUID tableId = UUID.randomUUID();
        insertTable(tableId, clubId);
        UUID bookingId = UUID.randomUUID();
        insertBooking(bookingId, userId, tableId);
        String filename = "receipt.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{1,2,3,4,5};
        UUID receiptId = receiptDao.create(bookingId, userId, filename, contentType, data);
        assertNotNull(receiptId);
        Optional<Receipt> receiptOpt = receiptDao.findByIdWithData(receiptId);
        assertTrue(receiptOpt.isPresent());
        Receipt receipt = receiptOpt.get();
        assertEquals(bookingId, receipt.getBookingId());
        assertEquals(userId, receipt.getUploadedBy());
        assertEquals(filename, receipt.getFilename());
        assertEquals(contentType, receipt.getContentType());
        assertArrayEquals(data, receipt.getData());
    }

    @Test
    public void testFindByBooking() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID clubId = UUID.randomUUID();
        insertClub(clubId, userId);
        UUID tableId = UUID.randomUUID();
        insertTable(tableId, clubId);
        UUID bookingId = UUID.randomUUID();
        insertBooking(bookingId, userId, tableId);
        String filename = "receipt2.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{10,20,30};
        receiptDao.create(bookingId, userId, filename, contentType, data);
        Optional<Receipt> receiptOpt = receiptDao.findByBooking(bookingId);
        assertTrue(receiptOpt.isPresent());
        Receipt receipt = receiptOpt.get();
        assertEquals(bookingId, receipt.getBookingId());
        assertEquals(filename, receipt.getFilename());
    }

    @Test
    public void testFindById() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID clubId = UUID.randomUUID();
        insertClub(clubId, userId);
        UUID tableId = UUID.randomUUID();
        insertTable(tableId, clubId);
        UUID bookingId = UUID.randomUUID();
        insertBooking(bookingId, userId, tableId);
        String filename = "receipt3.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{7,8,9};
        UUID receiptId = receiptDao.create(bookingId, userId, filename, contentType, data);
        Optional<Receipt> receiptOpt = receiptDao.findById(receiptId);
        assertTrue(receiptOpt.isPresent());
        Receipt receipt = receiptOpt.get();
        assertEquals(bookingId, receipt.getBookingId());
        assertEquals(filename, receipt.getFilename());
        assertNull(receipt.getData()); // findById no trae los datos binarios
    }

    @Test
    public void testDeleteByBooking() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID clubId = UUID.randomUUID();
        insertClub(clubId, userId);
        UUID tableId = UUID.randomUUID();
        insertTable(tableId, clubId);
        UUID bookingId = UUID.randomUUID();
        insertBooking(bookingId, userId, tableId);
        String filename = "receipt4.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{11,12,13};
        receiptDao.create(bookingId, userId, filename, contentType, data);
        boolean deleted = receiptDao.deleteByBooking(bookingId);
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "receipts"));
    }

    @Test
    public void testDeleteById() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID clubId = UUID.randomUUID();
        insertClub(clubId, userId);
        UUID tableId = UUID.randomUUID();
        insertTable(tableId, clubId);
        UUID bookingId = UUID.randomUUID();
        insertBooking(bookingId, userId, tableId);
        String filename = "receipt5.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{21,22,23};
        UUID receiptId = receiptDao.create(bookingId, userId, filename, contentType, data);
        boolean deleted = receiptDao.delete(receiptId);
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "receipts"));
    }
}
