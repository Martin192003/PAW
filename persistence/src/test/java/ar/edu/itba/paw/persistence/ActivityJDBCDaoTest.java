// persistence/src/test/java/ar/edu/itba/paw/persistence/ActivityJDBCDaoTest.java
package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Activity;
import ar.edu.itba.paw.model.Booking;
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
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class ActivityJDBCDaoTest {

    private static final String TYPE = "TEST_TYPE";
    private static final String DETAILS = "Detalles";

    @Autowired
    private DataSource ds;
    @Autowired
    private ActivityJDBCDao activityDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "activity");
    }

    @Test
    public void testCreate() {
        // Crear usuario
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                userId, "Test", "User", "test@example.com", "hash", "123456", "user", OffsetDateTime.now()
        );

        // Crear club
        UUID clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club Test", "Location", 5.0, 1, "medio", "desc", userId, "pop", OffsetDateTime.now()
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
                bookingId, userId, tableId, OffsetDateTime.now(), OffsetDateTime.now().plusHours(1), "confirmada", OffsetDateTime.now(), 100.0
        );

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        OffsetDateTime date = OffsetDateTime.now();

        Activity activity = activityDao.create(booking, TYPE, DETAILS, date);

        assertNotNull(activity);
        assertEquals(TYPE, activity.getActivityType());
        assertEquals(DETAILS, activity.getDetails());
        assertEquals(date, activity.getActivityDate());
        assertEquals(booking.getBookingId(), activity.getBooking().getBookingId());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "activity"));
    }
}