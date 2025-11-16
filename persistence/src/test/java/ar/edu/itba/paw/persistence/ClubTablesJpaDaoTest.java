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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ClubTablesJpaDaoTest {

    @Autowired
    private ClubTablesJpaDao clubTablesDao;

    @PersistenceContext
    private EntityManager em;

    private User user;
    private Club club;

    private ClubTable table1;
    private ClubTable table2;
    private ClubTable table3;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Booking b").executeUpdate();
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        user = persistUser();
        club = persistClub(user, "Club Main");

        table1 = persistTable(club, 1,
                TableState.DISPONIBLE,
                new BigDecimal("100.00")
        );
        table2 = persistTable(club, 2,
                TableState.DISPONIBLE,
                new BigDecimal("120.00")
        );
        table3 = persistTable(club, 3,
                TableState.RESERVADA,
                new BigDecimal("150.00")
        );

        em.flush();
    }

    // ===================== Tests create =====================

    @Test
    public void testCreate() {
        long before = countTables();

        ClubTable created = clubTablesDao.create(
                10,
                6,
                club.getClubId(),
                TableState.DISPONIBLE,
                new BigDecimal("300.00"),
                new BigDecimal("0.10")
        );
        em.flush();

        assertNotNull(created);
        assertNotNull(created.getTableId());
        assertEquals(club.getClubId(), created.getClubId());
        assertEquals(10, created.getTableNumber());
        assertEquals(6, created.getCapacity());
        assertEquals(TableState.DISPONIBLE, created.getTableState());
        assertEquals(0, new BigDecimal("300.00").compareTo(created.getPrice()));
        assertEquals(0, new BigDecimal("0.10").compareTo(created.getDiscount()));

        assertEquals(before + 1, countTables());
    }

    // ===================== Tests update =====================

    @Test
    public void testUpdate() {
        BigDecimal newPrice = new BigDecimal("500.00");
        BigDecimal newDiscount = new BigDecimal("0.25");

        ClubTable updated = clubTablesDao.update(
                table1.getTableId(),
                club.getClubId(),
                99,
                8,
                TableState.RESERVADA,
                newPrice,
                newDiscount
        );
        em.flush();

        assertNotNull(updated);
        assertEquals(table1.getTableId(), updated.getTableId());
        assertEquals(club.getClubId(), updated.getClubId());
        assertEquals(99, updated.getTableNumber());
        assertEquals(8, updated.getCapacity());
        assertEquals(TableState.RESERVADA, updated.getTableState());
        assertEquals(0, newPrice.compareTo(updated.getPrice()));
        assertEquals(0, newDiscount.compareTo(updated.getDiscount()));
    }

    @Test
    public void testUpdatePartialDoesNotOverrideNulls() {
        BigDecimal originalPrice = table2.getPrice();
        int originalCapacity = table2.getCapacity();

        ClubTable updated = clubTablesDao.update(
                table2.getTableId(),
                club.getClubId(),
                null,
                null,
                TableState.DISPONIBLE,
                null,
                BigDecimal.ONE
        );
        em.flush();

        assertNotNull(updated);
        assertEquals(originalCapacity, updated.getCapacity());
        assertEquals(0, originalPrice.compareTo(updated.getPrice()));
        assertEquals(TableState.DISPONIBLE, updated.getTableState());
        assertEquals(0, BigDecimal.ONE.compareTo(updated.getDiscount()));
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        ClubTable updated = clubTablesDao.update(
                UUID.randomUUID(),
                club.getClubId(),
                10,
                4,
                TableState.DISPONIBLE,
                BigDecimal.TEN,
                BigDecimal.ZERO
        );
        assertNull(updated);
    }

    // ===================== Tests delete =====================

    @Test
    public void testDelete() {
        long before = countTables();

        boolean deleted = clubTablesDao.delete(table3.getTableId());
        em.flush();

        assertTrue(deleted);
        assertEquals(before - 1, countTables());
        assertNull(em.find(ClubTable.class, table3.getTableId()));
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        long before = countTables();

        boolean deleted = clubTablesDao.delete(UUID.randomUUID());

        assertFalse(deleted);
        assertEquals(before, countTables());
    }

    // ===================== Tests findById =====================

    @Test
    public void testFindById() {
        Optional<ClubTable> found = clubTablesDao.findById(table1.getTableId());
        assertTrue(found.isPresent());
        assertEquals(table1.getTableId(), found.get().getTableId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<ClubTable> found = clubTablesDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ===================== Tests findByClub =====================

    @Test
    public void testFindByClubReturnsOnlyClubTablesSorted() {
        List<ClubTable> tables = clubTablesDao.findByClub(club);

        assertEquals(3, tables.size());
        assertEquals(1, tables.get(0).getTableNumber());
        assertEquals(2, tables.get(1).getTableNumber());
        assertEquals(3, tables.get(2).getTableNumber());

        tables.forEach(t -> assertEquals(club.getClubId(), t.getClubId()));
    }

    @Test
    public void testFindByClubEmptyWhenNoTables() {
        Club newClub = persistClub(user, "Empty Club");
        em.flush();

        List<ClubTable> tables = clubTablesDao.findByClub(newClub);
        assertTrue(tables.isEmpty());
    }

    // ===================== Tests disponibilidad (count + find) =====================

    @Test
    public void testCountAvailableByClubAndRange() {
        OffsetDateTime from = OffsetDateTime.now()
                .plusDays(1)
                .withHour(22).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime to = from.plusHours(4);

        createBooking(
                table2,
                from.plusMinutes(30),
                from.plusHours(2),
                Status.CONFIRMADA
        );

        createBooking(
                table3,
                from.minusHours(5),
                from.minusHours(3),
                Status.CONFIRMADA
        );

        em.flush();

        int count = clubTablesDao.countAvailableByClubAndRange(
                club.getClubId(), from, to);
        assertEquals(2, count);
    }

    @Test
    public void testFindAvailableByClubAndRange() {
        OffsetDateTime from = OffsetDateTime.now()
                .plusDays(1)
                .withHour(22).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime to = from.plusHours(4);

        createBooking(
                table2,
                from.plusMinutes(30),
                from.plusHours(2),
                Status.CONFIRMADA
        );

        createBooking(
                table3,
                from.minusHours(5),
                from.minusHours(3),
                Status.CONFIRMADA
        );

        em.flush();

        List<ClubTable> available = clubTablesDao.findAvailableByClubAndRange(
                club.getClubId(), from, to);

        assertEquals(2, available.size());
        List<UUID> ids = available.stream().map(ClubTable::getTableId).toList();
        assertTrue(ids.contains(table1.getTableId()));
        assertTrue(ids.contains(table3.getTableId()));
    }

    @Test
    public void testFindAvailableByClubAndRangePaginatedFirstPage() {
        OffsetDateTime from = OffsetDateTime.now()
                .plusDays(1)
                .withHour(22).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime to = from.plusHours(4);

        createBooking(
                table2,
                from.plusMinutes(10),
                from.plusHours(1),
                Status.PENDIENTE
        );
        em.flush();

        List<ClubTable> result = clubTablesDao.findAvailableByClubAndRangePaginated(
                club.getClubId(), from, to, 1, 0);
        assertEquals(1, result.size());
        assertEquals(table1.getTableId(), result.getFirst().getTableId());
    }

    @Test
    public void testFindAvailableByClubAndRangePaginatedSecondPage() {
        OffsetDateTime from = OffsetDateTime.now()
                .plusDays(1)
                .withHour(22).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime to = from.plusHours(4);

        createBooking(
                table2,
                from.plusMinutes(10),
                from.plusHours(1),
                Status.PENDIENTE
        );
        em.flush();

        List<ClubTable> result = clubTablesDao.findAvailableByClubAndRangePaginated(
                club.getClubId(), from, to, 1, 1);
        assertEquals(1, result.size());
        assertEquals(table3.getTableId(), result.getFirst().getTableId());
    }

    @Test
    public void testCountAvailableByClubAndRangeReturnsZeroWhenAllBlocked() {
        OffsetDateTime from = OffsetDateTime.now()
                .plusDays(1)
                .withHour(22).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime to = from.plusHours(4);

        createBooking(table1, from.plusMinutes(10), from.plusHours(1), Status.PENDIENTE);
        createBooking(table2, from.plusMinutes(5), from.plusHours(2), Status.CONFIRMADA);
        createBooking(table3, from, to.minusMinutes(1), Status.CONFIRMADA);
        em.flush();

        int count = clubTablesDao.countAvailableByClubAndRange(
                club.getClubId(), from, to);
        assertEquals(0, count);
    }

    @Test
    public void testFindAvailableByClubAndRangeReturnsEmptyWhenAllBlocked() {
        OffsetDateTime from = OffsetDateTime.now()
                .plusDays(1)
                .withHour(22).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime to = from.plusHours(4);

        createBooking(table1, from.plusMinutes(10), from.plusHours(1), Status.PENDIENTE);
        createBooking(table2, from.plusMinutes(5), from.plusHours(2), Status.CONFIRMADA);
        createBooking(table3, from, to.minusMinutes(1), Status.CONFIRMADA);
        em.flush();

        List<ClubTable> available = clubTablesDao.findAvailableByClubAndRange(
                club.getClubId(), from, to);
        assertTrue(available.isEmpty());
    }

    // ===================== Helpers =====================

    private User persistUser() {
        User u = new User(
                UUID.randomUUID(),
                "user",
                "Name",
                "Last",
                "email",
                "{noop}hash",
                "cell",
                Role.USER,
                OffsetDateTime.now(),
                null
        );
        em.persist(u);
        return u;
    }

    private Club persistClub(User contact, String clubName) {
        Club c = new Club(
                clubName,
                "Location",
                new BigDecimal("5.0"),
                1,
                null,          // priceRange
                "desc",
                contact,
                null,          // musicGenre
                OffsetDateTime.now(),
                null,          // zone
                Boolean.FALSE,
                null           // cbu
        );
        em.persist(c);
        return c;
    }

    private ClubTable persistTable(Club club,
                                   int tableNumber,
                                   TableState state,
                                   BigDecimal price) {
        ClubTable t = new ClubTable(
                null,
                club.getClubId(),
                tableNumber,
                4,
                state,
                price,
                BigDecimal.ZERO
        );
        em.persist(t);
        return t;
    }

    private long countTables() {
        return em.createQuery("select count(t) from ClubTable t", Long.class)
                .getSingleResult();
    }

    private void createBooking(ClubTable table,
                               OffsetDateTime start,
                               OffsetDateTime end,
                               Status status) {
        Booking b = new Booking();
        b.setBookingId(UUID.randomUUID());
        b.setUser(user);
        b.setTable(table);
        b.setBookingStart(start);
        b.setBookingEnd(end);
        b.setStatus(status);
        b.setTotalAmount(BigDecimal.TEN);
        b.setCreatedAt(OffsetDateTime.now());
        em.persist(b);
    }
}
