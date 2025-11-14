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
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        em.createQuery("DELETE FROM Booking b").executeUpdate();
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        user = persistUser("user", "name", "last", "email", "cell");
        Club club = persistClub(user);
        clubId = club.getClubId();
        ClubTable table = persistTable(club);
        tableId = table.getTableId();

        Status status = Status.PENDIENTE;
        BigDecimal totalAmount = new BigDecimal("200.00");
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);

        booking = persistBooking(user, table, status, totalAmount, start, end);
        em.flush();
    }

    // ----------------- Tests create -----------------

    @Test
    public void testCreate() {
        User u2 = persistUser("user2", "name2", "last2", "email2", "cell2");
        Club c2 = persistClub(u2);
        ClubTable t2 = persistTable(c2);

        Status status = Status.PENDIENTE;
        BigDecimal totalAmount = new BigDecimal("200.00");
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);

        Booking b2 = bookingsDao.create(u2, t2.getTableId(), status, totalAmount, start, end);
        em.flush();

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
    public void testCreateInvalidDatesThrows() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.minusHours(1);

        assertThrows(PersistenceException.class, () ->
                bookingsDao.create(user, tableId, Status.PENDIENTE, BigDecimal.TEN, start, end)
        );
    }

    // ----------------- Tests update -----------------

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
    public void testUpdateNotFoundReturnsEmpty() {
        UUID randomId = UUID.randomUUID();
        Status newStatus = Status.CONFIRMADA;
        BigDecimal newAmount = new BigDecimal("300.00");
        OffsetDateTime newStart = booking.getBookingStart().plusDays(1);
        OffsetDateTime newEnd = newStart.plusHours(2);

        Optional<Booking> updated = bookingsDao.update(
                randomId, user, tableId, newStatus, newAmount, newStart, newEnd
        );

        assertFalse(updated.isPresent());
    }

    // ----------------- Tests delete -----------------

    @Test
    public void testDelete() {
        boolean deleted = bookingsDao.delete(booking.getBookingId());
        em.flush();
        assertTrue(deleted);
        assertEquals(0L, countBookings());
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        boolean deleted = bookingsDao.delete(UUID.randomUUID());
        assertFalse(deleted);
        assertEquals(1L, countBookings());
    }

    // ----------------- Tests findById -----------------

    @Test
    public void testFindById() {
        Optional<Booking> found = bookingsDao.findById(booking.getBookingId());
        assertTrue(found.isPresent());
        assertEquals(booking.getBookingId(), found.get().getBookingId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Booking> found = bookingsDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ----------------- Tests updateStatus -----------------

    @Test
    public void testUpdateStatus() {
        Optional<Booking> updated = bookingsDao.updateStatus(booking.getBookingId(), Status.CONFIRMADA);
        assertTrue(updated.isPresent());
        assertEquals(Status.CONFIRMADA, updated.get().getStatus());
    }

    @Test
    public void testUpdateStatusNotFoundReturnsEmpty() {
        Optional<Booking> updated = bookingsDao.updateStatus(UUID.randomUUID(), Status.CONFIRMADA);
        assertFalse(updated.isPresent());
    }

    // ----------------- Tests findByUser -----------------

    @Test
    public void testFindByUser() {
        List<Booking> result = bookingsDao.findByUser(user.getId());
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    public void testFindByUserWithoutBookingsReturnsEmpty() {
        User other = persistUser("other", "n", "l", "other", "c");
        em.flush();

        assertTrue(bookingsDao.findByUser(other.getId()).isEmpty());
    }

    // ----------------- Tests findByTable -----------------

    @Test
    public void testFindByTable() {
        List<Booking> result = bookingsDao.findByTable(tableId);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    public void testFindByTableWithoutBookingsReturnsEmpty() {
        ClubTable otherTable = persistTable(persistClub(user));
        em.flush();

        assertTrue(bookingsDao.findByTable(otherTable.getTableId()).isEmpty());
    }

    // ----------------- Tests searchByClubIdsPaginated -----------------

    @Test
    public void testSearchByClubIdsPaginatedBasic() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchByClubIdsPaginated(
                Collections.singletonList(clubId),
                booking.getBookingId(),
                booking.getStatus(),
                "created_desc",
                date,
                null,
                10,
                0
        );

        assertEquals(1, result.size());
        assertEquals(booking.getBookingId(), result.getFirst().getBookingId());
    }

    @Test
    public void testSearchByClubIdsPaginatedEmptyWhenNoMatch() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchByClubIdsPaginated(
                Collections.singletonList(UUID.randomUUID()),
                booking.getBookingId(),
                booking.getStatus(),
                "created_desc",
                date,
                null,
                10,
                0
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchByClubIdsPaginatedEmptyWhenClubIdsNull() {
        List<Booking> result = bookingsDao.searchByClubIdsPaginated(
                null, null, null, null, null, null, 10, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchByClubIdsPaginatedEmptyWhenClubIdsEmpty() {
        List<Booking> result = bookingsDao.searchByClubIdsPaginated(
                Collections.emptyList(), null, null, null, null, null, 10, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchByClubIdsPaginatedFilterByHasReceiptTrue() {
        insertReceipt(UUID.randomUUID(), booking.getBookingId(), user.getId());
        em.flush();

        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchByClubIdsPaginated(
                Collections.singletonList(clubId),
                null,
                null,
                "created_desc",
                date,
                Boolean.TRUE,
                10,
                0
        );
        assertEquals(1, result.size());
        assertEquals(booking.getBookingId(), result.getFirst().getBookingId());
    }

    @Test
    public void testSearchByClubIdsPaginatedFilterByHasReceiptFalse() {
        List<Booking> result = bookingsDao.searchByClubIdsPaginated(
                Collections.singletonList(clubId),
                null,
                null,
                "created_desc",
                booking.getBookingStart().toLocalDate(),
                Boolean.FALSE,
                10,
                0
        );
        assertEquals(1, result.size());
        assertEquals(booking.getBookingId(), result.getFirst().getBookingId());
    }

    // ----------------- Tests searchPaginated -----------------

    @Test
    public void testSearchPaginatedBasic() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchPaginated(
                user.getId(),
                clubId,
                booking.getBookingId(),
                booking.getStatus(),
                "created_desc",
                date,
                null,
                10,
                0
        );

        assertEquals(1, result.size());
        assertEquals(booking.getBookingId(), result.getFirst().getBookingId());
    }

    @Test
    public void testSearchPaginatedEmptyWhenNoMatch() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchPaginated(
                UUID.randomUUID(),
                clubId,
                booking.getBookingId(),
                booking.getStatus(),
                "created_desc",
                date,
                null,
                10,
                0
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchPaginatedFilterByHasReceiptTrue() {
        insertReceipt(UUID.randomUUID(), booking.getBookingId(), user.getId());
        em.flush();

        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchPaginated(
                user.getId(),
                clubId,
                null,
                booking.getStatus(),
                "created_desc",
                date,
                Boolean.TRUE,
                10,
                0
        );
        assertEquals(1, result.size());
    }

    @Test
    public void testSearchPaginatedFilterByHasReceiptFalse() {
        insertReceipt(UUID.randomUUID(), booking.getBookingId(), user.getId());
        em.flush();

        LocalDate date = booking.getBookingStart().toLocalDate();

        List<Booking> result = bookingsDao.searchPaginated(
                user.getId(),
                clubId,
                null,
                booking.getStatus(),
                null,
                date,
                Boolean.FALSE,
                10,
                0
        );
        assertTrue(result.isEmpty());
    }

    // ----------------- Tests countByClubIds -----------------

    @Test
    public void testCountByClubIds() {
        int count = bookingsDao.countByClubIds(
                Collections.singletonList(clubId),
                null,
                null,
                null,
                null
        );
        assertEquals(1, count);
    }

    @Test
    public void testCountByClubIdsZeroWhenNull() {
        int count = bookingsDao.countByClubIds(null, null, null, null, null);
        assertEquals(0, count);
    }

    @Test
    public void testCountByClubIdsZeroWhenEmpty() {
        int count = bookingsDao.countByClubIds(Collections.emptyList(), null, null, null, null);
        assertEquals(0, count);
    }

    // ----------------- Tests countSearch -----------------

    @Test
    public void testCountSearchWithClubId() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        int count = bookingsDao.countSearch(
                user.getId(),
                clubId,
                booking.getBookingId(),
                booking.getStatus(),
                date,
                null
        );

        assertEquals(1, count);
    }

    @Test
    public void testCountSearchWithoutClubIdAndWithoutReceipt() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        int count = bookingsDao.countSearch(
                user.getId(),
                null,
                booking.getBookingId(),
                booking.getStatus(),
                date,
                null
        );

        assertEquals(1, count);
    }

    @Test
    public void testCountSearchWithHasReceiptFalse() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        int count = bookingsDao.countSearch(
                user.getId(),
                null,
                booking.getBookingId(),
                booking.getStatus(),
                date,
                Boolean.FALSE
        );
        assertEquals(1, count);
    }

    @Test
    public void testCountSearchWithHasReceiptTrueWhenNoReceipt() {
        LocalDate date = booking.getBookingStart().toLocalDate();

        int count = bookingsDao.countSearch(
                user.getId(),
                null,
                booking.getBookingId(),
                booking.getStatus(),
                date,
                Boolean.TRUE
        );
        assertEquals(0, count);
    }

    @Test
    public void testCountSearchWithHasReceiptTrueWhenHasReceipt() {
        insertReceipt(UUID.randomUUID(), booking.getBookingId(), user.getId());
        em.flush();

        LocalDate date = booking.getBookingStart().toLocalDate();

        int count = bookingsDao.countSearch(
                user.getId(),
                null,
                booking.getBookingId(),
                booking.getStatus(),
                date,
                Boolean.TRUE
        );
        assertEquals(1, count);
    }

    @Test
    public void testCountSearchWithHasReceiptFalseWhenHasReceipt() {
        insertReceipt(UUID.randomUUID(), booking.getBookingId(), user.getId());
        em.flush();

        LocalDate date = booking.getBookingStart().toLocalDate();

        int count = bookingsDao.countSearch(
                user.getId(),
                null,
                booking.getBookingId(),
                booking.getStatus(),
                date,
                Boolean.FALSE
        );
        assertEquals(0, count);
    }

    // ----------------- Tests existsPastConfirmedByUserAndClub -----------------

    @Test
    public void testExistsPastConfirmedByUserAndClubTrueWhenPastConfirmed() {
        booking.setStatus(Status.CONFIRMADA);
        booking.setBookingEnd(OffsetDateTime.now().minusHours(1));
        em.flush();

        boolean exists = bookingsDao.existsPastConfirmedByUserAndClub(
                user.getId(),
                clubId,
                OffsetDateTime.now()
        );

        assertTrue(exists);
    }

    @Test
    public void testExistsPastConfirmedByUserAndClubFalseWhenNoPastConfirmed() {
        booking.setStatus(Status.PENDIENTE);
        booking.setBookingEnd(OffsetDateTime.now().plusHours(1));
        em.flush();

        boolean exists = bookingsDao.existsPastConfirmedByUserAndClub(
                user.getId(),
                clubId,
                OffsetDateTime.now()
        );

        assertFalse(exists);
    }

    // ----------------- Tests mapClubNamesForBookings -----------------

    @Test
    public void testMapClubNamesForBookings() {
        Map<UUID, String> map = bookingsDao.mapClubNamesForBookings(
                Collections.singletonList(booking.getBookingId())
        );

        assertEquals(1, map.size());
        assertEquals("Club Test", map.get(booking.getBookingId()));
    }

    @Test
    public void testMapClubNamesForBookingsEmptyWhenNull() {
        Map<UUID, String> result = bookingsDao.mapClubNamesForBookings(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapClubNamesForBookingsEmptyWhenEmpty() {
        Map<UUID, String> result = bookingsDao.mapClubNamesForBookings(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapClubNamesForBookingsIgnoresUnknownIds() {
        UUID unknown = UUID.randomUUID();
        Map<UUID, String> map = bookingsDao.mapClubNamesForBookings(
                Arrays.asList(booking.getBookingId(), unknown)
        );

        assertEquals(1, map.size());
        assertTrue(map.containsKey(booking.getBookingId()));
        assertFalse(map.containsKey(unknown));
    }

    // ----------------- Helpers -------------------

    private User persistUser(String username, String name, String surname, String email, String cell) {
        User u = new User(
                UUID.randomUUID(),
                username, name, surname, email,
                "{noop}hash",
                cell,
                Role.USER,
                OffsetDateTime.now(),
                null // locale: not needed for tests, pass null
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
                null,
                "desc",
                contact,
                null,
                OffsetDateTime.now(),
                null,
                Boolean.FALSE,
                null
        );
        em.persist(c);
        return c;
    }

    private ClubTable persistTable(Club club) {
        ClubTable t = new ClubTable(
                null,
                club.getClubId(),
                1, 4,
                TableState.DISPONIBLE,
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
        em.persist(t);
        return t;
    }

    private Booking persistBooking(User user,
                                   ClubTable table,
                                   Status status,
                                   BigDecimal totalAmount,
                                   OffsetDateTime start,
                                   OffsetDateTime end) {
        Booking b = new Booking();
        b.setBookingId(UUID.randomUUID());
        b.setUser(user);
        b.setTable(table);
        b.setStatus(status);
        b.setTotalAmount(totalAmount);
        b.setBookingStart(start);
        b.setBookingEnd(end);
        b.setCreatedAt(OffsetDateTime.now());
        em.persist(b);
        return b;
    }

    private long countBookings() {
        return em.createQuery("select count(b) from Booking b", Long.class).getSingleResult();
    }

    private void insertReceipt(UUID receiptId, UUID bookingId, UUID uploadedBy) {
        em.createNativeQuery("INSERT INTO receipts (receipt_id, booking_id, uploaded_by, url, content_type, created_at, data, filename) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)")
                .setParameter(1, receiptId)
                .setParameter(2, bookingId)
                .setParameter(3, uploadedBy)
                .setParameter(4, "http://example.com/receipt")
                .setParameter(5, "image/jpeg")
                .setParameter(6, new byte[]{1, 2, 3})
                .setParameter(7, "receipt.jpg")
                .executeUpdate();
    }

}
