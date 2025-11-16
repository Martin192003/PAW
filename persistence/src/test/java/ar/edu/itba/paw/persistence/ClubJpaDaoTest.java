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
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ClubJpaDaoTest {

    @Autowired
    private ClubJpaDao clubDao;

    @PersistenceContext
    private EntityManager em;

    private User owner;
    private User otherUser;
    private User customer;
    private Club club;
    private ClubTable table;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Booking b").executeUpdate();
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        owner = persistUser("owner", "Owner", "owner@example.com", "123");
        otherUser = persistUser("other", "Other", "other@example.com", "456");
        customer = persistUser("customer", "Customer", "customer@example.com", "789");

        club = persistClub(owner,
                "Club Test",
                "Descripcion"
        );
        em.flush();

        table = persistTable(club);
        em.flush();
    }



    // ----------------- Tests create -----------------

    @Test
    public void testCreate() {
        User contact = persistUser("contact", "Contact", "contact@example.com", "000");

        Club created = clubDao.create(
                "New Club",
                "Nueva desc",
                "Nueva loc",
                null,
                contact,
                null,
                null,
                Boolean.TRUE,
                "CBU-123"
        );
        em.flush();

        assertNotNull(created);
        assertNotNull(created.getClubId());
        assertEquals("New Club", created.getClubName());
        assertEquals("Nueva loc", created.getLocation());
        assertEquals("Nueva desc", created.getDescription());
        assertEquals(contact.getId(), created.getContactInfo().getId());
        assertEquals(2L, countClubs());
    }

    // ----------------- Tests update -----------------

    @Test
    public void testUpdate() {
        BigDecimal newRating = new BigDecimal("4.50");
        Integer newQty = 5;
        String newName = "Club Updated";
        String newLoc = "New Location";
        String newDesc = "New Desc";
        String newCbu = "CBU-999";

        Club updated = clubDao.update(
                club.getClubId(),
                newName,
                newLoc,
                newRating,
                newQty,
                null,
                newDesc,
                otherUser.getId(),
                null,
                null,
                true,
                newCbu
        );

        assertNotNull(updated);
        assertEquals(newName, updated.getClubName());
        assertEquals(newLoc, updated.getLocation());
        assertEquals(newDesc, updated.getDescription());
        assertEquals(newRating, updated.getRating());
        assertEquals(newQty, updated.getQuantityRatings());
        assertEquals(otherUser.getId(), updated.getContactInfo().getId());
        assertEquals(true, updated.getIsBar());
        assertEquals(newCbu, updated.getCbu());
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        Club updated = clubDao.update(
                UUID.randomUUID(),
                "Name",
                "Loc",
                BigDecimal.ONE,
                1,
                null,
                "Desc",
                owner.getId(),
                null,
                null,
                Boolean.TRUE,
                "CBU"
        );

        assertNull(updated);
    }

    // ----------------- Tests delete -----------------

    @Test
    public void testDelete() {
        boolean deleted = clubDao.delete(club.getClubId());
        em.flush();

        assertTrue(deleted);
        assertEquals(0L, countClubs());
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        boolean deleted = clubDao.delete(UUID.randomUUID());
        assertFalse(deleted);
        assertEquals(1L, countClubs());
    }

    // ----------------- Tests findById -----------------

    @Test
    public void testFindById() {
        Optional<Club> found = clubDao.findById(club.getClubId());
        assertTrue(found.isPresent());
        assertEquals(club.getClubId(), found.get().getClubId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Club> found = clubDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ----------------- Tests findByUserId / paginated / countByUserId / findIdsByAdmin -----------------

    @Test
    public void testFindByUserIdReturnsClubsWhereIsContact() {
        List<Club> clubs = clubDao.findByUserId(owner.getId());
        assertEquals(1, clubs.size());
        assertEquals(club.getClubId(), clubs.getFirst().getClubId());
    }

    @Test
    public void testFindByUserIdEmptyForUserWithoutClubs() {
        List<Club> clubs = clubDao.findByUserId(otherUser.getId());
        assertTrue(clubs.isEmpty());
    }

    @Test
    public void testCountByUserId() {
        persistClubJPA(owner);
        em.flush();

        int total = clubDao.countByUserId(owner.getId());
        assertEquals(2, total);
    }

    @Test
    public void testFindByUserIdPaginatedFirstPage() {
        Club second = persistClubJPA(owner);
        em.flush();

        List<Club> result = clubDao.findByUserIdPaginated(owner.getId(), 1, 0);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getClubId().equals(club.getClubId()) ||
                result.getFirst().getClubId().equals(second.getClubId()));
    }

    @Test
    public void testFindByUserIdPaginatedSecondPage() {
        Club second = persistClubJPA(owner);
        em.flush();

        List<Club> result = clubDao.findByUserIdPaginated(owner.getId(), 1, 1);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getClubId().equals(club.getClubId()) ||
                result.getFirst().getClubId().equals(second.getClubId()));
    }

    @Test
    public void testFindIdsByAdminIncludesContactInfo() {
        List<UUID> result = clubDao.findIdsByAdmin(owner.getId());
        assertEquals(1, result.size());
        assertEquals(club.getClubId(), result.getFirst());
    }

    @Test
    public void testFindIdsByAdminEmptyForNonAdmin() {
        List<UUID> result = clubDao.findIdsByAdmin(otherUser.getId());
        assertTrue(result.isEmpty());
    }

    // ----------------- Tests findAdminsByClubId -----------------

    @Test
    public void testFindAdminsByClubIdReturnsContactWhenNoAdmins() {
        List<User> admins = clubDao.findAdminsByClubId(club.getClubId());
        assertEquals(1, admins.size());
        assertEquals(owner.getId(), admins.getFirst().getId());
    }

    // ----------------- Tests findClubsWithBookingsByUserId -----------------

    @Test
    public void testFindClubsWithBookingsByUserId() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusHours(2);
        persistBooking(customer, table, start, end, new BigDecimal("200.00"));
        em.flush();

        List<Club> result = clubDao.findClubsWithBookingsByUserId(customer.getId());
        assertEquals(1, result.size());
        assertEquals(club.getClubId(), result.getFirst().getClubId());
    }

    @Test
    public void testFindClubsWithBookingsByUserIdEmptyForUserWithoutBookings() {
        List<Club> result = clubDao.findClubsWithBookingsByUserId(otherUser.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSearchPaginatedEmptyWhenNoMatch() {
        List<Club> result = clubDao.searchPaginated(
                "no-existe",
                null,
                null,
                null,
                null,
                "created_desc",
                10,
                0
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCountSearchWithNameLike() {
        persistClubJPA(owner);
        em.flush();

        int count = clubDao.countSearch("club", null, null, null, null);
        assertEquals(1, count);
    }

    @Test
    public void testCountSearchWithNameLikeFullMatch() {
        int count = clubDao.countSearch("club test", null, null, null, null);
        assertEquals(1, count);
    }

    @Test
    public void testCountSearchWithNameLikeNoMatch() {
        int count = clubDao.countSearch("no-existe", null, null, null, null);
        assertEquals(0, count);
    }

    @Test
    public void testUpdateRatingAndCount() {
        BigDecimal rating = new BigDecimal("4.2");
        int quantity = 7;

        clubDao.updateRatingAndCount(club.getClubId(), rating, quantity);
        em.flush();

        Club reloaded = em.find(Club.class, club.getClubId());
        assertEquals(rating, reloaded.getRating());
        assertEquals(Integer.valueOf(quantity), reloaded.getQuantityRatings());
    }

    // ----------------- Helpers -----------------

    private User persistUser(String username, String name, String email, String cell) {
        User u = new User(
                UUID.randomUUID(),
                username,
                name,
                "Last",
                email,
                "{noop}hash",
                cell,
                Role.USER,
                OffsetDateTime.now(),
                null
        );
        em.persist(u);
        return u;
    }


    private Club persistClub(User contact,
                             String name,
                             String description) {
        Club c = new Club(
                name,
                "Location",
                null,
                null,
                null,
                description,
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

    private Club persistClubJPA(User contact) {
        return persistClub(
                contact,
                "Otro Nombre",
                "desc"
        );
    }

    private ClubTable persistTable(Club club) {
        ClubTable t = new ClubTable(
                null,
                club.getClubId(),
                1,
                4,
                TableState.DISPONIBLE,
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
        em.persist(t);
        return t;
    }

    private void persistBooking(User user, ClubTable t,
                                OffsetDateTime start,
                                OffsetDateTime end,
                                BigDecimal totalAmount) {
        Booking b = new Booking();
        b.setBookingId(UUID.randomUUID());
        b.setUser(user);
        b.setTable(t);
        b.setStatus(Status.CONFIRMADA);
        b.setBookingStart(start);
        b.setBookingEnd(end);
        b.setTotalAmount(totalAmount);
        b.setCreatedAt(OffsetDateTime.now());
        em.persist(b);
    }

    private long countClubs() {
        return em.createQuery("select count(c) from Club c", Long.class)
                .getSingleResult();
    }
}
