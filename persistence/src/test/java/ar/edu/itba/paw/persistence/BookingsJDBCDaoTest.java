package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class BookingsJDBCDaoTest {

    @Autowired
    private DataSource ds;
    @Autowired
    private BookingsJDBCDao bookingsDao;
    @Autowired
    private UserJDBCDao userDao;
    private JdbcTemplate jdbcTemplate;
    private PasswordEncoder passwordEncoder;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "bookings");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    public void testCreate() {
        // Crear usuario
        User user = userDao.create("user", "name",passwordEncoder.encode("pass"), "last", "email", "cell", Role.USER);

        // Crear club
        UUID clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club Test", "Location", 5.0, 1, "medio", "desc", user.getId(), "pop", OffsetDateTime.now()
        );

        // Crear mesa asociada al club
        UUID tableId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableId, clubId, 1, 4, "disponible", 100.0, 0.0
        );

        Status status = Status.PENDIENTE;
        BigDecimal totalAmount = BigDecimal.valueOf(200);
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);

        // Crear booking
        Booking booking = bookingsDao.create(user, tableId, status, totalAmount, start, end);

        assertNotNull(booking);
        assertEquals(user.getId(), booking.getUser().getId());
        assertEquals(tableId, booking.getTableId());
        assertEquals(status, booking.getStatus());
        assertEquals(0, totalAmount.compareTo(booking.getTotalAmount()));
        assertEquals(start.truncatedTo(ChronoUnit.SECONDS), booking.getBookingStart().truncatedTo(ChronoUnit.SECONDS)); //Revisar lo de ChronoUnit
        assertEquals(end.truncatedTo(ChronoUnit.SECONDS), booking.getBookingEnd().truncatedTo(ChronoUnit.SECONDS)); //Revisar lo de ChronoUnit
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "bookings"));
    }
}
