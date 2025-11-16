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
import javax.persistence.PersistenceException;
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
    private Club club;
    private UUID clubId;

    private UUID photoId;

    private Event pastEvent;
    private Event todayEvent;
    private Event futureEvent;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Event e").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.createNativeQuery("DELETE FROM photos").executeUpdate();
        em.flush();

        user = persistUser("user", "name", "last", "email", "cell");
        club = persistClub(user);
        clubId = club.getClubId();

        photoId = UUID.randomUUID();
        insertPhoto(photoId, clubId, user.getId());

        LocalDate today = LocalDate.now();

        pastEvent = persistEvent(UUID.randomUUID(), clubId,
                "Past Event", "Past description",
                today.minusDays(2), photoId);

        todayEvent = persistEvent(UUID.randomUUID(), clubId,
                "Today Event", "Today description",
                today, null);

        futureEvent = persistEvent(UUID.randomUUID(), clubId,
                "Future Event", "Future description",
                today.plusDays(2), null);

        em.flush();
    }

    // ----------------------------------------------------------------
    // create
    // ----------------------------------------------------------------

    @Test
    public void testCreate() {
        LocalDate date = LocalDate.now().plusDays(5);
        UUID newPhotoId = UUID.randomUUID();
        insertPhoto(newPhotoId, clubId, user.getId());

        Event created = eventDao.create(
                clubId,
                "New Event",
                "New description",
                date,
                newPhotoId
        );
        em.flush();
        em.clear();

        assertNotNull(created);
        assertNotNull(created.getEventId());
        assertEquals(clubId, created.getClubId());
        assertEquals("New Event", created.getEventName());
        assertEquals("New description", created.getDescription());
        assertEquals(date, created.getDate());
        assertEquals(newPhotoId, created.getPhotoId());

        assertEquals(4L, countEvents());
    }

    @Test
    public void testCreateWithInvalidClubThrows() {
        LocalDate date = LocalDate.now();

        assertThrows(PersistenceException.class, () -> {
            eventDao.create(
                    UUID.randomUUID(),
                    "Invalid Event",
                    "Should fail",
                    date,
                    null
            );
            em.flush();
        });
    }

    // ----------------------------------------------------------------
    // update
    // ----------------------------------------------------------------

    @Test
    public void testUpdate() {
        LocalDate newDate = todayEvent.getDate().plusDays(10);
        UUID newPhotoId = UUID.randomUUID();
        insertPhoto(newPhotoId, clubId, user.getId());

        Event updated = eventDao.update(
                todayEvent.getEventId(),
                clubId,
                "Updated name",
                "Updated description",
                newDate,
                newPhotoId
        );

        assertNotNull(updated);
        assertEquals(todayEvent.getEventId(), updated.getEventId());
        assertEquals("Updated name", updated.getEventName());
        assertEquals("Updated description", updated.getDescription());
        assertEquals(newDate, updated.getDate());
        assertEquals(newPhotoId, updated.getPhotoId());
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        Event updated = eventDao.update(
                UUID.randomUUID(),
                clubId,
                "Does not exist",
                "No description",
                LocalDate.now(),
                null
        );

        assertNull(updated);
    }

    // ----------------------------------------------------------------
    // delete
    // ----------------------------------------------------------------

    @Test
    public void testDelete() {
        boolean deleted = eventDao.delete(todayEvent.getEventId());
        em.flush();
        em.clear();

        assertTrue(deleted);
        assertEquals(2L, countEvents());
        assertNull(em.find(Event.class, todayEvent.getEventId()));
    }
    @Test
    public void testDeleteNotFoundReturnsFalse() {
        boolean deleted = eventDao.delete(UUID.randomUUID());

        assertFalse(deleted);
        assertEquals(3L, countEvents());
    }

    // ----------------------------------------------------------------
    // findById
    // ----------------------------------------------------------------

    @Test
    public void testFindById() {
        Optional<Event> found = eventDao.findById(pastEvent.getEventId());
        assertTrue(found.isPresent());
        assertEquals(pastEvent.getEventId(), found.get().getEventId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Event> found = eventDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ----------------------------------------------------------------
    // findByClub (paginado)
    // ----------------------------------------------------------------

    @Test
    public void testFindByClubReturnsAllOrderedByDateDesc() {
        List<Event> result = eventDao.findByClub(clubId, 0, 10);

        assertEquals(3, result.size());
        assertEquals(futureEvent.getEventId(), result.get(0).getEventId());
        assertEquals(todayEvent.getEventId(), result.get(1).getEventId());
        assertEquals(pastEvent.getEventId(), result.get(2).getEventId());
    }

    @Test
    public void testFindByClubPaginatesPage0() {
        List<Event> page0 = eventDao.findByClub(clubId, 0, 2);
        assertEquals(2, page0.size());
        assertEquals(futureEvent.getEventId(), page0.get(0).getEventId());
        assertEquals(todayEvent.getEventId(), page0.get(1).getEventId());
    }

    @Test
    public void testFindByClubPaginatesPage1() {
        List<Event> page1 = eventDao.findByClub(clubId, 1, 2);
        assertEquals(1, page1.size());
        assertEquals(pastEvent.getEventId(), page1.get(0).getEventId());
    }

    @Test
    public void testFindByClubPaginatesPage2() {
        List<Event> page2 = eventDao.findByClub(clubId, 2, 2);
        assertTrue(page2.isEmpty());
    }

    @Test
    public void testFindByClubWithZeroSizeReturnsEmpty() {
        List<Event> result = eventDao.findByClub(clubId, 0, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByClubWithoutEventsReturnsEmpty() {
        UUID otherClubId = UUID.randomUUID();
        // Insertamos club sin eventos
        Club otherClub = new Club(
                "Other Club",
                "Other Location",
                new BigDecimal("5.0"),
                0,
                null,
                "desc",
                user,
                null,
                OffsetDateTime.now(),
                null,
                Boolean.FALSE,
                null
        );
        em.persist(otherClub);
        em.flush();

        List<Event> result = eventDao.findByClub(otherClub.getClubId(), 0, 10);
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // countByClub
    // ----------------------------------------------------------------

    @Test
    public void testCountByClub() {
        int count = eventDao.countByClub(clubId);
        assertEquals(3, count);
    }

    @Test
    public void testCountByClubZeroWhenNoEvents() {
        UUID otherClubId = UUID.randomUUID();
        Club otherClub = new Club(
                "Empty Club",
                "Location",
                new BigDecimal("4.0"),
                0,
                null,
                "desc",
                user,
                null,
                OffsetDateTime.now(),
                null,
                Boolean.FALSE,
                null
        );
        em.persist(otherClub);
        em.flush();

        int count = eventDao.countByClub(otherClub.getClubId());
        assertEquals(0, count);
    }

    // ----------------------------------------------------------------
    // findUpcoming (global)
    // ----------------------------------------------------------------

    @Test
    public void testFindUpcomingFromTodayReturnsTodayAndFuture() {
        LocalDate today = LocalDate.now();

        List<Event> result = eventDao.findUpcoming(today, 0, 10);

        assertEquals(2, result.size());
        assertEquals(todayEvent.getEventId(), result.get(0).getEventId());
        assertEquals(futureEvent.getEventId(), result.get(1).getEventId());
    }

    @Test
    public void testFindUpcomingFromFutureDateReturnsOnlyFuture() {
        LocalDate from = LocalDate.now().plusDays(1);

        List<Event> result = eventDao.findUpcoming(from, 0, 10);

        assertEquals(1, result.size());
        assertEquals(futureEvent.getEventId(), result.get(0).getEventId());
    }

    @Test
    public void testFindUpcomingWithNullFromReturnsEmpty() {
        List<Event> result = eventDao.findUpcoming(null, 0, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindUpcomingWithZeroSizeReturnsEmpty() {
        List<Event> result = eventDao.findUpcoming(LocalDate.now(), 0, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindUpcomingPaginatesPage0() {
        LocalDate today = LocalDate.now();

        List<Event> page0 = eventDao.findUpcoming(today, 0, 1);
        assertEquals(1, page0.size());
        assertEquals(todayEvent.getEventId(), page0.get(0).getEventId());
    }

    @Test
    public void testFindUpcomingPaginatesPage1() {
        LocalDate today = LocalDate.now();

        List<Event> page1 = eventDao.findUpcoming(today, 1, 1);
        assertEquals(1, page1.size());
        assertEquals(futureEvent.getEventId(), page1.get(0).getEventId());
    }

    @Test
    public void testFindUpcomingPaginatesPage2Empty() {
        LocalDate today = LocalDate.now();

        List<Event> page2 = eventDao.findUpcoming(today, 2, 1);
        assertTrue(page2.isEmpty());
    }

    // ----------------------------------------------------------------
    // countUpcoming (global)
    // ----------------------------------------------------------------

    @Test
    public void testCountUpcomingFromToday() {
        LocalDate today = LocalDate.now();
        int count = eventDao.countUpcoming(today);
        assertEquals(2, count);
    }

    @Test
    public void testCountUpcomingFromAfterFutureReturnsZero() {
        LocalDate from = LocalDate.now().plusDays(10);
        int count = eventDao.countUpcoming(from);
        assertEquals(0, count);
    }

    // ----------------------------------------------------------------
    // findByPhotoIdAndClub
    // ----------------------------------------------------------------

    @Test
    public void testFindByPhotoIdAndClubFound() {
        Optional<Event> result = eventDao.findByPhotoIdAndClub(photoId, clubId);

        assertTrue(result.isPresent());
        assertEquals(pastEvent.getEventId(), result.get().getEventId());
    }

 @Test
 public void testFindByPhotoIdAndClubNotFoundByPhoto() {
     Optional<Event> result = eventDao.findByPhotoIdAndClub(UUID.randomUUID(), clubId);
     assertFalse(result.isPresent());
 }

 @Test
 public void testFindByPhotoIdAndClubNotFoundByClub() {
     Optional<Event> result = eventDao.findByPhotoIdAndClub(photoId, UUID.randomUUID());
     assertFalse(result.isPresent());
 }
    // ----------------------------------------------------------------
    // attachPhoto
    // ----------------------------------------------------------------

    @Test
    public void testAttachPhotoUpdatesEventPhoto() {
        UUID newPhotoId = UUID.randomUUID();
        insertPhoto(newPhotoId, clubId, user.getId());

        eventDao.attachPhoto(todayEvent.getEventId(), newPhotoId);
        em.flush();
        em.clear();

        Event reloaded = em.find(Event.class, todayEvent.getEventId());
        assertNotNull(reloaded);
        assertEquals(newPhotoId, reloaded.getPhotoId());
    }

    @Test
    public void testAttachPhotoNotFoundThrows() {
        UUID newPhotoId = UUID.randomUUID();
        insertPhoto(newPhotoId, clubId, user.getId());

        assertThrows(IllegalArgumentException.class, () ->
                eventDao.attachPhoto(UUID.randomUUID(), newPhotoId)
        );
    }

    // ----------------------------------------------------------------
    // findUpcomingByClub
    // ----------------------------------------------------------------

    @Test
    public void testFindUpcomingByClubReturnsTodayAndFutureForClub() {
        List<Event> result = eventDao.findUpcomingByClub(clubId, 0, 10);

        assertEquals(2, result.size());
        assertEquals(todayEvent.getEventId(), result.get(0).getEventId());
        assertEquals(futureEvent.getEventId(), result.get(1).getEventId());
    }

@Test
public void testFindUpcomingByClubPaginatesPage0() {
    // page 0: today
    List<Event> page0 = eventDao.findUpcomingByClub(clubId, 0, 1);
    assertEquals(1, page0.size());
    assertEquals(todayEvent.getEventId(), page0.get(0).getEventId());
}

@Test
public void testFindUpcomingByClubPaginatesPage1() {
    // page 1: future
    List<Event> page1 = eventDao.findUpcomingByClub(clubId, 1, 1);
    assertEquals(1, page1.size());
    assertEquals(futureEvent.getEventId(), page1.get(0).getEventId());
}

@Test
public void testFindUpcomingByClubPaginatesPage2Empty() {
    // page 2: empty
    List<Event> page2 = eventDao.findUpcomingByClub(clubId, 2, 1);
    assertTrue(page2.isEmpty());
}

    @Test
    public void testFindUpcomingByClubWithNullClubReturnsEmpty() {
        List<Event> result = eventDao.findUpcomingByClub(null, 0, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindUpcomingByClubWithZeroSizeReturnsEmpty() {
        List<Event> result = eventDao.findUpcomingByClub(clubId, 0, 0);
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // findPastByClub
    // ----------------------------------------------------------------

    @Test
    public void testFindPastByClubReturnsOnlyPastEvents() {
        List<Event> result = eventDao.findPastByClub(clubId, 0, 10);

        assertEquals(1, result.size());
        assertEquals(pastEvent.getEventId(), result.get(0).getEventId());
    }

  @Test
  public void testFindPastByClubPage0ReturnsPastEvent() {
      List<Event> page0 = eventDao.findPastByClub(clubId, 0, 1);
      assertEquals(1, page0.size());
      assertEquals(pastEvent.getEventId(), page0.get(0).getEventId());
  }

  @Test
  public void testFindPastByClubPage1IsEmpty() {
      List<Event> page1 = eventDao.findPastByClub(clubId, 1, 1);
      assertTrue(page1.isEmpty());
  }

    @Test
    public void testFindPastByClubWithNullClubReturnsEmpty() {
        List<Event> result = eventDao.findPastByClub(null, 0, 10);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindPastByClubWithZeroSizeReturnsEmpty() {
        List<Event> result = eventDao.findPastByClub(clubId, 0, 0);
        assertTrue(result.isEmpty());
    }

    // ----------------------------------------------------------------
    // countUpcomingByClub / countPastByClub
    // ----------------------------------------------------------------

    @Test
    public void testCountUpcomingByClub() {
        int count = eventDao.countUpcomingByClub(clubId);
        assertEquals(2, count);
    }

    @Test
    public void testCountPastByClub() {
        int count = eventDao.countPastByClub(clubId);
        assertEquals(1, count);
    }

  @Test
  public void testCountUpcomingByClubWithoutEvents() {
      Club otherClub = new Club(
              "No Events Club - Upcoming",
              "Location",
              new BigDecimal("4.5"),
              0,
              null,
              "desc",
              user,
              null,
              OffsetDateTime.now(),
              null,
              Boolean.FALSE,
              null
      );
      em.persist(otherClub);
      em.flush();

      assertEquals(0, eventDao.countUpcomingByClub(otherClub.getClubId()));
  }

  @Test
  public void testCountPastByClubWithoutEvents() {
      Club otherClub = new Club(
              "No Events Club - Past",
              "Location",
              new BigDecimal("4.5"),
              0,
              null,
              "desc",
              user,
              null,
              OffsetDateTime.now(),
              null,
              Boolean.FALSE,
              null
      );
      em.persist(otherClub);
      em.flush();

      assertEquals(0, eventDao.countPastByClub(otherClub.getClubId()));
  }
    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private User persistUser(String username, String name, String surname, String email, String cell) {
        User u = new User(
                UUID.randomUUID(),
                username,
                name,
                surname,
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

    private Event persistEvent(UUID eventId,
                               UUID clubId,
                               String name,
                               String description,
                               LocalDate date,
                               UUID photoId) {
        Event e = new Event(eventId, clubId, name, description, date, photoId);
        em.persist(e);
        return e;
    }

    private long countEvents() {
        return em.createQuery("SELECT COUNT(e) FROM Event e", Long.class)
                .getSingleResult();
    }

    private void insertPhoto(UUID photoId, UUID clubId, UUID postedBy) {
        em.createNativeQuery("""
                INSERT INTO photos (photo_id, club_id, url, posted_by, created_at, data, filename, content_type)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?)
                """)
                .setParameter(1, photoId)
                .setParameter(2, clubId)
                .setParameter(3, "http://example.com/photo")
                .setParameter(4, postedBy)
                .setParameter(5, new byte[]{1, 2, 3})
                .setParameter(6, "photo.jpg")
                .setParameter(7, "image/jpeg")
                .executeUpdate();
    }
}
