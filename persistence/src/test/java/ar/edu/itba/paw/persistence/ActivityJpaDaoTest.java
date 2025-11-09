/*package ar.edu.itba.paw.persistence;

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
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ActivityJpaDaoTest {

    private static final String TYPE = "TEST_TYPE";
    private static final String DETAILS = "Detalles";

    @Autowired
    private ActivityJpaDao activityDao;

    @PersistenceContext
    private EntityManager em;

    /*@Before
    public void clean() {
        em.createQuery("DELETE FROM Activity a").executeUpdate();
        em.flush();
    }


    private User persistUser() {
        User u = new User(
                UUID.randomUUID(),
                "testuser",
                "Test",
                "User",
                "test@example.com",
                "hash",
                "123456",
                Role.USER,
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
                1,
                4,
                TableState.DISPONIBLE,
                new BigDecimal("100.00"),
                BigDecimal.ZERO
        );
        em.persist(t);
        return t;
    }

    private Booking persistBooking(User u, ClubTable t) {
        Booking b = new Booking(
                UUID.randomUUID(), // Booking NO genera id: lo seteamos acá
                u,
                t,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                Status.CONFIRMADA, // Ajusta al valor existente en tu enum
                OffsetDateTime.now(),
                new BigDecimal("100.00")
        );
        em.persist(b);
        return b;
    }

    private Booking persistedBookingGraph() {
        User u = persistUser();
        Club c = persistClub(u);
        ClubTable t = persistTable(c);
        Booking b = persistBooking(u, t);
        em.flush();
        return b;
    }

    private long countActivities() {
        return em.createQuery("select count(a) from Activity a", Long.class)
                .getSingleResult();
    }

    // ------------------------
    // Tests
    // ------------------------

    @Test
    public void testCreate() {
        Booking booking = persistedBookingGraph();
        OffsetDateTime date = OffsetDateTime.now();

        Activity activity = activityDao.create(booking, TYPE, DETAILS, date);

        assertNotNull(activity);
        assertEquals(TYPE, activity.getActivityType());
        assertEquals(DETAILS, activity.getDetails());
        assertEquals(date, activity.getActivityDate());
        assertEquals(booking.getBookingId(), activity.getBooking().getBookingId());
        assertEquals(1L, countActivities());
    }

    @Test
    public void testUpdateSuccess() {
        Booking booking = persistedBookingGraph();
        Activity activity = activityDao.create(booking, TYPE, DETAILS, OffsetDateTime.now());

        String newType = "UPDATED_TYPE";
        String newDetails = "Nuevos detalles";
        OffsetDateTime newDate = activity.getActivityDate().plusDays(1);

        Activity updated = activityDao.update(activity.getActivityId(), booking, newType, newDetails, newDate);

        assertNotNull(updated);
        assertEquals(newType, updated.getActivityType());
        assertEquals(newDetails, updated.getDetails());
        assertEquals(newDate, updated.getActivityDate());
    }

    @Test
    public void testDeleteSuccess() {
        Booking booking = persistedBookingGraph();
        Activity activity = activityDao.create(booking, TYPE, DETAILS, OffsetDateTime.now());

        boolean deleted = activityDao.delete(activity.getActivityId());

        assertTrue(deleted);
        assertEquals(0L, countActivities());
    }

    @Test
    public void testFindByIdSuccess() {
        Booking booking = persistedBookingGraph();
        Activity activity = activityDao.create(booking, TYPE, DETAILS, OffsetDateTime.now());

        Optional<Activity> found = activityDao.findById(activity.getActivityId());

        assertTrue(found.isPresent());
        assertEquals(activity.getActivityId(), found.get().getActivityId());
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        assertTrue(activityDao.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    public void testFindAllWithActivities() {
        Booking booking = persistedBookingGraph();
        OffsetDateTime date = OffsetDateTime.now();

        activityDao.create(booking, TYPE, DETAILS, date);
        activityDao.create(booking, "OTRO_TYPE", "OTROS", date.plusMinutes(1));

        assertEquals(2, activityDao.findAll().size());
    }

    @Test
    public void testFindByUser() {
        Booking booking = persistedBookingGraph();
        activityDao.create(booking, TYPE, DETAILS, OffsetDateTime.now());

        assertEquals(1, activityDao.findByUser(booking.getUser()).size());
    }

    @Test
    public void testDeleteReturnsFalseIfNotFound() {
        assertFalse(activityDao.delete(UUID.randomUUID()));
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        Booking booking = persistedBookingGraph();
        Activity updated = activityDao.update(UUID.randomUUID(), booking, "type", "details", OffsetDateTime.now());
        assertNull(updated);
    }
}
*/