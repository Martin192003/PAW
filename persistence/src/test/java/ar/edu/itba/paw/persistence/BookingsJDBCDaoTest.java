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
    private BookingsJpaDao bookingsDao;
    @Autowired
    private UserJpaDao userDao;
    private JdbcTemplate jdbcTemplate;
    private PasswordEncoder passwordEncoder;

    private UUID clubId;
    private UUID tableId;
    private User user;
    private Booking booking;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "bookings");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "club_tables");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        passwordEncoder = new BCryptPasswordEncoder();
        // Crear usuario
        user = userDao.create("user", "name", passwordEncoder.encode("pass"), "last", "email", "cell", Role.USER);
        // Crear club
        clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club Test", "Location", 5.0, 1, "medio", "desc", user.getId(), "pop", OffsetDateTime.now()
        );
        // Crear mesa asociada al club
        tableId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableId, clubId, 1, 4, "disponible", 100.0, 0.0
        );
        // Crear booking
        Status status = Status.PENDIENTE;
        BigDecimal totalAmount = BigDecimal.valueOf(200);
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);
        booking = bookingsDao.create(user, tableId, status, totalAmount, start, end);
    }

    @Test
    public void testCreate() {
        // Crear usuario con datos únicos
        User user = userDao.create("user2", "name2", passwordEncoder.encode("pass2"), "last2", "email2", "cell2", Role.USER);

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
        assertEquals(start.truncatedTo(ChronoUnit.SECONDS), booking.getBookingStart().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(end.truncatedTo(ChronoUnit.SECONDS), booking.getBookingEnd().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "bookings"));
    }

    @Test
    public void testUpdate() {
        Status newStatus = Status.CONFIRMADA;
        BigDecimal newAmount = BigDecimal.valueOf(300);
        OffsetDateTime newStart = booking.getBookingStart().plusDays(1);
        OffsetDateTime newEnd = newStart.plusHours(2);
        Optional<Booking> updated = bookingsDao.update(booking.getBookingId(), user, tableId, newStatus, newAmount, newStart, newEnd);
        assertNotNull(updated);
        assertEquals(newStatus, updated.orElseThrow().getStatus());
        assertEquals(0, newAmount.compareTo(updated.orElseThrow().getTotalAmount()));
        assertEquals(newStart.truncatedTo(ChronoUnit.SECONDS), updated.orElseThrow().getBookingStart().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(newEnd.truncatedTo(ChronoUnit.SECONDS), updated.orElseThrow().getBookingEnd().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    public void testDelete() {
        boolean deleted = bookingsDao.delete(booking.getBookingId());
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "bookings"));
    }

    @Test
    public void testFindById() {
        Optional<Booking> found = bookingsDao.findById(booking.getBookingId());
        assertTrue(found.isPresent());
        assertEquals(booking.getBookingId(), found.get().getBookingId());
    }

    @Test
    public void testFindAll() {
        assertFalse(bookingsDao.findAll().isEmpty());
        assertEquals(1, bookingsDao.findAll().size());
    }

    @Test
    public void testFindByUser() {
        assertFalse(bookingsDao.findByUser(user.getId()).isEmpty());
        assertEquals(1, bookingsDao.findByUser(user.getId()).size());
    }

    @Test
    public void testFindByTable() {
        assertFalse(bookingsDao.findByTable(tableId).isEmpty());
        assertEquals(1, bookingsDao.findByTable(tableId).size());
    }

    @Test
    public void testFindAllPaginated() {
        assertEquals(1, bookingsDao.findAllPaginated(10, 0).size());
    }

    @Test
    public void testCountAll() {
        assertEquals(1, bookingsDao.countAll());
    }

    @Test
    public void testSearchPaginated() {
        assertEquals(1, bookingsDao.searchPaginated(user.getId(), clubId, booking.getBookingId(), booking.getStatus(), "created_desc", 10, 0).size());
    }

    @Test
    public void testCountSearch() {
        assertEquals(1, bookingsDao.countSearch(user.getId(), clubId, booking.getBookingId(), booking.getStatus()));
    }
}
