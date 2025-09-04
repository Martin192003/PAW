package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.BookingItem;
import ar.edu.itba.paw.model.Product;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class BookingItemsJDBCDaoTest {

    @Autowired
    private DataSource ds;
    @Autowired
    private BookingItemsJDBCDao bookingItemsDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "booking_items");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    @Test
    public void testCreate() {
        // Crear usuario
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "Test", "User", "test@example.com", "hash", "123456", "user", java.time.OffsetDateTime.now()
        );

        // Crear club
        UUID clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club Test", "Location", 5.0, 1, "medio", "desc", userId, "pop", java.time.OffsetDateTime.now()
        );

        // Crear mesa
        UUID tableId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableId, clubId, 1, 4, "disponible", 100.0, 0.0
        );

        // Crear booking
        UUID bookingId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO bookings (booking_id, user_id, table_id, booking_start, booking_end, status, created_at, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                bookingId, userId, tableId, java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now().plusHours(1), "confirmada", java.time.OffsetDateTime.now(), 100.0
        );
        Booking booking = new Booking();
        booking.setBookingId(bookingId);

        // Crear producto
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO products (product_id, club_id, product_name, description, base_price, product_type, is_available, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                productId, clubId, "Producto Test", "desc", 100.0, "alcoholicas", true, java.time.OffsetDateTime.now()
        );
        Product product = new Product();
        product.setProductId(productId);

        // Crear BookingItem con line_total
        int quantity = 2;
        BigDecimal unitPrice = BigDecimal.valueOf(100);
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        BookingItem item = bookingItemsDao.create(booking, product, quantity, unitPrice);

        assertNotNull(item);
        assertEquals(booking.getBookingId(), item.getBooking().getBookingId());
        assertEquals(product.getProductId(), item.getProduct().getProductId());
        assertEquals(quantity, item.getQuantity().intValue());
        assertEquals(unitPrice, item.getUnitPrice());
        assertEquals(lineTotal, item.getLineTotal());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "booking_items"));
    }
}