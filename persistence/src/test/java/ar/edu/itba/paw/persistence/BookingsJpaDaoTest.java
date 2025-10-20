package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class BookingsJpaDaoTest {

    @Autowired
    private BookingsJpaDao bookingsDao;

    @PersistenceContext
    private EntityManager em;

    private UUID clubId;
    private UUID tableId;
    private User user;
    private Booking booking;

    @Before
    public void setUp() {
        // Limpieza ligera (con @Transactional ya hay rollback por test)
        em.createQuery("DELETE FROM Booking b").executeUpdate();
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        // Arrange básico usando JPA
        user = persistUser("user", "name", "last", "email", "cell", Role.USER);
        Club club = persistClub(user);
        clubId = club.getClubId();
        ClubTable table = persistTable(club);
        tableId = table.getTableId();

        Status status = Status.PENDIENTE;
        BigDecimal totalAmount = new BigDecimal("200.00");
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);

        booking = bookingsDao.create(user, tableId, status, totalAmount, start, end);
        em.flush();
    }

    // ----------------- Helpers JPA -----------------

    private User persistUser(String username, String name, String surname, String email, String cell, Role role) {
        User u = new User(
                UUID.randomUUID(),
                username, name, surname, email,
                "{noop}hash", // no hace falta encriptar para el test
                cell,
                role,
                OffsetDateTime.now()
        );
        em.persist(u);
        return u;
    }

    private Club persistClub(User contact) {
        Club c = new Club(
                "Club Test",
                "Location",
                new BigDecimal("5.0"),
                1,
                null,          // Price (opcional)
                "desc",
                contact,
                null,          // MusicalGenre (opcional)
                OffsetDateTime.now(),
                null,          // Zone (opcional)
                Boolean.FALSE,
                null
        );
        em.persist(c);
        return c;
    }

    private ClubTable persistTable(Club club) {
        ClubTable t = new ClubTable(
                null,                   // UUID se autogenera en @PrePersist si null
                club.getClubId(),       // FK por UUID (no ManyToOne)
                1, 4,
                TableState.DISPONIBLE,
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
        em.persist(t);
        return t;
    }

    private long countBookings() {
        return em.createQuery("select count(b) from Booking b", Long.class).getSingleResult();
    }

    // ----------------- Tests -----------------

    @Test
    public void testCreate() {
        User u2 = persistUser("user2", "name2", "last2", "email2", "cell2", Role.USER);
        Club c2 = persistClub(u2);
        ClubTable t2 = persistTable(c2);

        Status status = Status.PENDIENTE;
        BigDecimal totalAmount = new BigDecimal("200.00");
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);


        Booking b2 = bookingsDao.create(u2, t2.getTableId(), status, totalAmount, start, end);


        assertNotNull(b2);
        assertEquals(u2.getId(), b2.getUser().getId());
        assertEquals(t2.getTableId(), b2.getTable().getTableId());
        assertEquals(status, b2.getStatus());
        assertEquals(0, totalAmount.compareTo(b2.getTotalAmount()));
        assertEquals(start.truncatedTo(ChronoUnit.SECONDS), b2.getBookingStart().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(end.truncatedTo(ChronoUnit.SECONDS), b2.getBookingEnd().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(2L, countBookings());
    }

    @Test
    public void testUpdate() {
        Status newStatus = Status.CONFIRMADA;
        BigDecimal newAmount = new BigDecimal("300.00");
        OffsetDateTime newStart = booking.getBookingStart().plusDays(1);
        OffsetDateTime newEnd = newStart.plusHours(2);

        Optional<Booking> updated = bookingsDao.update(
                booking.getBookingId(), user, tableId, newStatus, newAmount, newStart, newEnd
        );

        assertTrue(updated.isPresent());
        Booking u = updated.get();
        assertEquals(newStatus, u.getStatus());
        assertEquals(0, newAmount.compareTo(u.getTotalAmount()));
        assertEquals(newStart.truncatedTo(ChronoUnit.SECONDS), u.getBookingStart().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(newEnd.truncatedTo(ChronoUnit.SECONDS), u.getBookingEnd().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    public void testDelete() {
        boolean deleted = bookingsDao.delete(booking.getBookingId());
        assertTrue(deleted);
        assertEquals(0L, countBookings());
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
        assertEquals(1, bookingsDao.searchPaginated(user.getId(), clubId, booking.getBookingId(),
                booking.getStatus(), "created_desc", 10, 0).size());
    }

    @Test
    public void testCountSearch() {
        assertEquals(1, bookingsDao.countSearch(user.getId(), clubId, booking.getBookingId(), booking.getStatus()));
    }
}
