package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class EventJpaDaoTest {

    @Autowired
    private EventJpaDao eventDao;

    @PersistenceContext
    private EntityManager em;

    private User user;
    private Club clubMain;
    private Club clubOther;

    private Event evPastMain;
    private Event evTodayMain;
    private Event evFutureMain;
    private Event evPastOther;
    private Event evFutureOther;

    private LocalDate today;

    @Before
    public void setUp() {
        // Limpieza (por las dudas, aunque hay rollback por test)
        em.createQuery("DELETE FROM Event e").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        today = LocalDate.now();

        user = persistUser();
        clubMain = persistClub(user, "Main Club");
        clubOther = persistClub(user, "Other Club");

        evPastMain   = persistEvent(clubMain, "PastMain", today.minusDays(2));
        evTodayMain  = persistEvent(clubMain, "TodayMain", today);
        evFutureMain = persistEvent(clubMain, "FutureMain", today.plusDays(2));

        evPastOther   = persistEvent(clubOther, "PastOther", today.minusDays(1));
        evFutureOther = persistEvent(clubOther, "FutureOther", today.plusDays(1));

        em.flush();
    }


    // ----------------------------- create -----------------------------

    @Test
    public void testCreate() {
        long before = countEvents();

        UUID clubId = clubMain.getClubId();
        UUID photoId = UUID.randomUUID();
        LocalDate date = today.plusDays(5);

        Event created = eventDao.create(clubId, "NewEvent", "new desc", date, photoId);
        em.flush();

        assertNotNull(created);
        assertNotNull(created.getEventId());
        assertEquals(clubId, created.getClubId());
        assertEquals("NewEvent", created.getEventName());
        assertEquals("new desc", created.getDescription());
        assertEquals(date, created.getDate());
        assertEquals(photoId, created.getPhotoId());

        assertEquals(before + 1, countEvents());
    }

    // ----------------------------- update -----------------------------

    @Test
    public void testUpdate() {
        UUID newClubId = clubOther.getClubId();
        UUID newPhotoId = UUID.randomUUID();
        LocalDate newDate = today.plusDays(10);

        Event updated = eventDao.update(
                evPastMain.getEventId(),
                newClubId,
                "UpdatedName",
                "UpdatedDesc",
                newDate,
                newPhotoId
        );
        em.flush();

        assertNotNull(updated);
        assertEquals(evPastMain.getEventId(), updated.getEventId());
        assertEquals(newClubId, updated.getClubId());
        assertEquals("UpdatedName", updated.getEventName());
        assertEquals("UpdatedDesc", updated.getDescription());
        assertEquals(newDate, updated.getDate());
        assertEquals(newPhotoId, updated.getPhotoId());
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        Event updated = eventDao.update(
                UUID.randomUUID(),
                clubMain.getClubId(),
                "Name",
                "Desc",
                today,
                null
        );
        assertNull(updated);
    }

    // ----------------------------- delete -----------------------------

    @Test
    public void testDelete() {
        long before = countEvents();

        boolean deleted = eventDao.delete(evTodayMain.getEventId());
        em.flush();

        assertTrue(deleted);
        assertEquals(before - 1, countEvents());
        assertNull(em.find(Event.class, evTodayMain.getEventId()));
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        long before = countEvents();

        boolean deleted = eventDao.delete(UUID.randomUUID());

        assertFalse(deleted);
        assertEquals(before, countEvents());
    }

    // ----------------------------- findById -----------------------------

    @Test
    public void testFindById() {
        Optional<Event> found = eventDao.findById(evFutureMain.getEventId());
        assertTrue(found.isPresent());
        assertEquals(evFutureMain.getEventId(), found.get().getEventId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Event> found = eventDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ----------------------------- findByClub (paginado) -----------------------------

    @Test
    public void testFindByClubPaginatedFirstPageOrdersByDateDesc() {
        List<Event> page0 = eventDao.findByClub(clubMain.getClubId(), 0, 2);

        assertEquals(2, page0.size());
        assertEquals(evFutureMain.getDate(), page0.get(0).getDate());
        assertEquals(evTodayMain.getDate(),  page0.get(1).getDate());
    }

    @Test
    public void testFindByClubPaginatedSecondPageOrdersByDateDesc() {
        List<Event> page1 = eventDao.findByClub(clubMain.getClubId(), 1, 2);

        assertEquals(1, page1.size());
        assertEquals(evPastMain.getDate(), page1.getFirst().getDate());
    }

    @Test
    public void testFindByClubEmptyWhenNoEvents() {
        Club emptyClub = persistClub(user, "Empty Club");
        em.flush();

        List<Event> events = eventDao.findByClub(emptyClub.getClubId(), 0, 10);
        assertTrue(events.isEmpty());
    }

    @Test
    public void testFindByClubSizeZeroReturnsEmpty() {
        List<Event> events = eventDao.findByClub(clubMain.getClubId(), 0, 0);
        assertTrue(events.isEmpty());
    }

    // ----------------------------- searchByDateRangePaginated -----------------------------

    @Test
    public void testSearchByDateRangePaginatedBasic() {
        LocalDate start = today.minusDays(2);
        LocalDate end   = today;

        List<Event> result = eventDao.searchByDateRangePaginated(
                start,
                end,
                0,
                10
        );

        assertEquals(3, result.size());
        assertEquals(evPastMain.getDate(),   result.get(0).getDate());
        assertEquals(evPastOther.getDate(),  result.get(1).getDate());
        assertEquals(evTodayMain.getDate(),  result.get(2).getDate());
    }

    @Test
    public void testSearchByDateRangePaginatedEmptyWhenNoMatch() {
        LocalDate start = today.plusDays(20);
        LocalDate end   = today.plusDays(30);

        List<Event> result = eventDao.searchByDateRangePaginated(
                start, end, 0, 10
        );
        assertTrue(result.isEmpty());
    }

    // ----------------------------- countByDateRange / countByClub -----------------------------

    @Test
    public void testCountByDateRange() {
        LocalDate start = today.minusDays(2);
        LocalDate end   = today;

        int count = eventDao.countByDateRange(start, end);
        assertEquals(3, count);
    }

    @Test
    public void testCountByClub() {
        int mainCount  = eventDao.countByClub(clubMain.getClubId());
        int otherCount = eventDao.countByClub(clubOther.getClubId());

        assertEquals(3, mainCount);
        assertEquals(2, otherCount);
    }

    // ----------------------------- findUpcoming / countUpcoming -----------------------------

    @Test
    public void testFindUpcomingFromToday() {
        List<Event> upcoming = eventDao.findUpcoming(today, 0, 10);

        // upcoming (>= today): evTodayMain, evFutureOther, evFutureMain
        assertEquals(3, upcoming.size());
        assertEquals(today,             upcoming.get(0).getDate());
        assertEquals(today.plusDays(1), upcoming.get(1).getDate());
        assertEquals(today.plusDays(2), upcoming.get(2).getDate());
    }

    @Test
    public void testFindUpcomingPaginatedFirstPage() {
        List<Event> page0 = eventDao.findUpcoming(today, 0, 2);

        assertEquals(2, page0.size());
        assertEquals(today,             page0.get(0).getDate());
        assertEquals(today.plusDays(1), page0.get(1).getDate());
    }

    @Test
    public void testFindUpcomingPaginatedSecondPage() {
        List<Event> page1 = eventDao.findUpcoming(today, 1, 2);

        assertEquals(1, page1.size());
        assertEquals(today.plusDays(2), page1.getFirst().getDate());
    }

    @Test
    public void testCountUpcomingFromToday() {
        int count = eventDao.countUpcoming(today);
        assertEquals(3, count);
    }

    // ----------------------------- findByPhotoIdAndClub -----------------------------

    @Test
    public void testFindByPhotoIdAndClubNotFoundWhenNoEventHasPhoto() {
        Optional<Event> notFound = eventDao.findByPhotoIdAndClub(
                UUID.randomUUID(), clubMain.getClubId()
        );
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testFindByPhotoIdAndClubNotFoundWhenPhotoBelongsToOtherClub() {
        UUID photoId = UUID.randomUUID();

        Event managed = em.find(Event.class, evFutureOther.getEventId());
        managed.setPhotoId(photoId);
        em.flush();

        Optional<Event> wrongClub = eventDao.findByPhotoIdAndClub(photoId, clubMain.getClubId());
        assertFalse(wrongClub.isPresent());
    }

    // ----------------------------- attachPhoto -----------------------------

    @Test
    public void testAttachPhotoUpdatesPhotoId() {
        UUID newPhoto = UUID.randomUUID();

        eventDao.attachPhoto(evPastMain.getEventId(), newPhoto);
        em.flush();

        Event reloaded = em.find(Event.class, evPastMain.getEventId());
        assertEquals(newPhoto, reloaded.getPhotoId());
    }

    @Test
    public void testAttachPhotoThrowsWhenEventNotFound() {
        UUID randomEventId = UUID.randomUUID();
        UUID randomPhotoId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                eventDao.attachPhoto(randomEventId, randomPhotoId)
        );
    }

    // ----------------------------- upcoming/past by club -----------------------------

    @Test
    public void testFindUpcomingByClub() {
        List<Event> upcomingMain = eventDao.findUpcomingByClub(clubMain.getClubId(), 0, 10);

        assertEquals(2, upcomingMain.size());
        assertEquals(today,             upcomingMain.get(0).getDate());
        assertEquals(today.plusDays(2), upcomingMain.get(1).getDate());
        upcomingMain.forEach(e -> assertEquals(clubMain.getClubId(), e.getClubId()));
    }

    @Test
    public void testFindPastByClub() {
        List<Event> pastMain = eventDao.findPastByClub(clubMain.getClubId(), 0, 10);

        assertEquals(1, pastMain.size());
        assertEquals(evPastMain.getDate(), pastMain.getFirst().getDate());
        assertEquals(clubMain.getClubId(), pastMain.getFirst().getClubId());
    }

    @Test
    public void testCountUpcomingByClub() {
        int mainUpcoming  = eventDao.countUpcomingByClub(clubMain.getClubId());

        assertEquals(2, mainUpcoming);
    }

    @Test
    public void testCountPastByClub() {
        int mainPast  = eventDao.countPastByClub(clubMain.getClubId());

        assertEquals(1, mainPast);
    }

    // ----------------------------- Helpers -----------------------------

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


    private Event persistEvent(Club club, String name, LocalDate date) {
        Event e = new Event();
        e.setEventId(UUID.randomUUID());
        e.setClubId(club.getClubId());
        e.setEventName(name);
        e.setDescription("desc");
        e.setDate(date);
        e.setPhotoId(null);
        em.persist(e);
        return e;
    }

    private long countEvents() {
        return em.createQuery("SELECT COUNT(e) FROM Event e", Long.class)
                .getSingleResult();
    }

}
