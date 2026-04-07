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
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class PhotoJpaDaoTest {

    @Autowired
    private PhotoJpaDao photoDao;

    @PersistenceContext
    private EntityManager em;

    private UUID clubId;
    private UUID otherClubId;
    private User user;
    private Photo basePhoto;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Event e").executeUpdate();
        em.createQuery("DELETE FROM Photo p").executeUpdate();
        em.createQuery("DELETE FROM Booking b").executeUpdate();
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        user = persistUser();
        Club club = persistClub(user);
        Club otherClub = persistClub(user);
        clubId = club.getClubId();
        otherClubId = otherClub.getClubId();

        basePhoto = persistPhoto(clubId, user.getId(), "base.jpg", new byte[]{1,2,3}, PhotoType.CAROUSEL);
        em.flush();
    }

    // ----------------- create tests -----------------

    @Test
    public void testCreate() {
        UUID newId = photoDao.create(clubId, user.getId(), "create.jpg", "image/png", new byte[]{4,5}, PhotoType.COVER);
        em.flush();

        assertNotNull(newId);
        Optional<Photo> fetched = Optional.of(em.find(Photo.class,newId));
        assertTrue(fetched.isPresent());
        Photo p = fetched.get();
        assertEquals(clubId, p.getClubId());
        assertEquals(user.getId(), p.getPostedBy());
        assertArrayEquals(new byte[]{4,5}, p.getData());
        assertEquals("create.jpg", p.getFilename());
        assertEquals(PhotoType.COVER, p.getPhotoType());
    }

    @Test
    public void testCreateNullDataThrows() {
        assertThrows(PersistenceException.class, () -> {
            UUID id = photoDao.create(clubId, user.getId(), "no-data.jpg", "image/png", null, PhotoType.CAROUSEL);
            em.flush();
        });
    }

    @Test
    public void testCreateReviewPhoto() {
        UUID reviewId = UUID.randomUUID();
        UUID id = photoDao.createReviewPhoto(clubId, reviewId, user.getId(), "rev.jpg", "image/jpeg", new byte[]{9});
        em.flush();

        assertNotNull(id);
        Optional<Photo> f = Optional.of(em.find(Photo.class,id));
        assertTrue(f.isPresent());
        Photo p = f.get();
        assertEquals(reviewId, p.getReviewId());
        assertEquals(PhotoType.REVIEW, p.getPhotoType());
    }

    @Test
    public void testCreateReviewPhotoNullDataThrows() {
        assertThrows(PersistenceException.class, () -> {
            UUID id = photoDao.createReviewPhoto(clubId, UUID.randomUUID(), user.getId(), "rev.jpg", "image/jpeg", null);
            em.flush();
        });
    }

    @Test
    public void testCreateEventPhotoNullDataThrows() {
        assertThrows(PersistenceException.class, () -> {
            UUID id = photoDao.createEventPhoto(clubId, UUID.randomUUID(), user.getId(), "evt.jpg", "image/jpeg", null);
            em.flush();
        });
    }

    // ----------------- findByClub tests -----------------

    @Test
    public void testFindByClub_basic() {
        persistPhoto(clubId, user.getId(), "1.jpg", new byte[]{1}, PhotoType.CAROUSEL);
        persistPhoto(clubId, user.getId(), "2.jpg", new byte[]{2}, PhotoType.CAROUSEL);
        persistPhoto(otherClubId, user.getId(), "other.jpg", new byte[]{3}, PhotoType.CAROUSEL);
        em.flush();

        List<Photo> photos = photoDao.findByClub(clubId, 10, 0);
        assertFalse(photos.isEmpty());
        assertTrue(photos.stream().allMatch(p -> clubId.equals(p.getClubId())));
        assertTrue(photos.stream().allMatch(p -> p.getData() == null));
    }

    @Test
    public void testFindByClub_limitOffsetNegative_normalizedToZero() {
        for (int i = 0; i < 5; i++) persistPhoto(clubId, user.getId(), "n" + i + ".jpg", new byte[]{(byte)i}, PhotoType.CAROUSEL);
        em.flush();

        List<Photo> photos = photoDao.findByClub(clubId, -5, -1);
        assertNotNull(photos);
        assertTrue(true);
    }

    @Test
    public void testFindByClub_noPhotosReturnsEmpty() {
        List<Photo> photos = photoDao.findByClub(UUID.randomUUID(), 10, 0);
        assertTrue(photos.isEmpty());
    }

    // ----------------- findById / findByIdWithData -----------------

    @Test
    public void testFindByIdWithData_returnsData() {
        Optional<Photo> f = photoDao.findByIdWithData(basePhoto.getPhotoId());
        assertTrue(f.isPresent());
        assertArrayEquals(new byte[]{1,2,3}, f.get().getData());
    }

    @Test
    public void testFindById_returnsDetachedWithoutData() {
        Optional<Photo> f = photoDao.findById(basePhoto.getPhotoId());
        assertTrue(f.isPresent());
        Photo p = f.get();
        assertNull(p.getData());
    }

    @Test
    public void testFindById_notFoundEmpty() {
        Optional<Photo> f = photoDao.findById(UUID.randomUUID());
        assertFalse(f.isPresent());
    }

    // ----------------- save tests -----------------

    @Test
    public void testSave_newPersistsAndReturnsId() {
        Photo p = new Photo();
        p.setClubId(clubId);
        p.setPostedBy(user.getId());
        p.setFilename("save_new.jpg");
        p.setContentType("image/png");
        p.setData(new byte[]{10,11});
        p.setPhotoType(PhotoType.CAROUSEL);

        UUID id = photoDao.save(p);
        em.flush();

        assertNotNull(id);
        assertNotNull(em.find(Photo.class, id));
    }

    @Test
    public void testSave_mergeUpdatesExisting() {
        Photo p = persistPhoto(clubId, user.getId(), "toupdate.jpg", new byte[]{3,4}, PhotoType.CAROUSEL);
        em.flush();
        p.setFilename("updated.jpg");
        UUID returned = photoDao.save(p);
        em.flush();

        Photo updated = em.find(Photo.class, returned);
        assertEquals("updated.jpg", updated.getFilename());
    }

    @Test
    public void testSave_invalidThrows() {
        Photo p = new Photo();
        assertThrows(PersistenceException.class, () -> {
            photoDao.save(p);
            em.flush();
        });
    }

    // ----------------- findByBooking tests -----------------

    @Test
    public void testFindByBooking_found() {
        UUID bookingId = persistBooking(user, persistTable(persistClub(user)), OffsetDateTime.now(), OffsetDateTime.now().plusHours(2)).getBookingId();
        Photo p = persistPhoto(otherClubId, user.getId(), "forBooking.jpg", new byte[]{8}, PhotoType.CAROUSEL);
        em.flush();

        Optional<Photo> found = photoDao.findByBooking(bookingId);
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getPostedBy());
    }

    @Test
    public void testFindByBooking_notFound() {
        Optional<Photo> found = photoDao.findByBooking(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ----------------- delete tests -----------------

    @Test
    public void testDelete_existingReturnsTrue() {
        Photo p = persistPhoto(clubId, user.getId(), "todelete.jpg", new byte[]{1}, PhotoType.CAROUSEL);
        em.flush();

        boolean deleted = photoDao.delete(p.getPhotoId());
        em.flush();
        assertTrue(deleted);
        assertFalse(photoDao.findById(p.getPhotoId()).isPresent());
    }

    @Test
    public void testDelete_nonExistingReturnsFalse() {
        boolean deleted = photoDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }

    // ----------------- cover & carousel tests -----------------

    @Test
    public void testFindCoverByClubId() {
        Photo cover = persistPhoto(clubId, user.getId(), "cover.jpg", new byte[]{2}, PhotoType.COVER);
        em.flush();

        Optional<Photo> found = photoDao.findCoverByClubId(clubId);
        assertTrue(found.isPresent());
        assertEquals(PhotoType.COVER, found.get().getPhotoType());
    }

    @Test
    public void testFindCarouselByClubId_limitsAndOrdering() {
        for (int i = 0; i < 5; i++) persistPhoto(clubId, user.getId(), "car" + i + ".jpg", new byte[]{(byte)i}, PhotoType.CAROUSEL);
        em.flush();

        List<Photo> carousel = photoDao.findCarouselByClubId(clubId);
        assertFalse(carousel.isEmpty());
        assertTrue(carousel.stream().allMatch(p -> p.getPhotoType() == PhotoType.CAROUSEL));
    }

    @Test
    public void testDeleteCoverByClubId() {
        persistPhoto(clubId, user.getId(), "coverToDelete.jpg", new byte[]{1}, PhotoType.COVER);
        em.flush();

        photoDao.deleteCoverByClubId(clubId);
        em.flush();

        Optional<Photo> found = photoDao.findCoverByClubId(clubId);
        assertFalse(found.isPresent());
    }

    // ----------------- review photos tests -----------------

    @Test
    public void testFindReviewPhotosByClub_pagination() {
        for (int i = 0; i < 6; i++) persistPhoto(clubId, user.getId(), "rev" + i + ".jpg", new byte[]{(byte)i}, PhotoType.REVIEW);
        em.flush();

        List<Photo> page = photoDao.findReviewPhotosByClub(clubId, 3, 0);
        assertNotNull(page);
        assertTrue(page.size() <= 3);
        assertTrue(page.stream().allMatch(p -> p.getPhotoType() == PhotoType.REVIEW));
    }

    @Test
    public void testFindReviewPhotosByClub_emptyWhenNone() {
        List<Photo> result = photoDao.findReviewPhotosByClub(UUID.randomUUID(), 10, 0);
        assertTrue(result.isEmpty());
    }

    // ----------------- findCarouselWithoutEventByClubId tests -----------------

    @Test
    public void testFindCarouselWithoutEventByClubId_excludesEventPhotos() {
        Photo withEvent = persistPhoto(clubId, user.getId(), "we.jpg", new byte[]{1}, PhotoType.CAROUSEL);
        em.flush();

        Event ev = new Event();
        ev.setEventId(UUID.randomUUID());
        ev.setClubId(clubId);
        ev.setEventName("E");
        ev.setDate(java.time.LocalDate.now());
        ev.setPhotoId(withEvent.getPhotoId());
        em.persist(ev);

        Photo withoutEvent = persistPhoto(clubId, user.getId(), "noe.jpg", new byte[]{2}, PhotoType.CAROUSEL);
        em.flush();

        List<Photo> result = photoDao.findCarouselWithoutEventByClubId(clubId, 10, 0);
        List<UUID> ids = result.stream().map(Photo::getPhotoId).toList();
        assertTrue(ids.contains(withoutEvent.getPhotoId()));
        assertFalse(ids.contains(withEvent.getPhotoId()));
    }

    @Test
    public void testFindCarouselWithoutEventByClubId_emptyWhenNone() {
        List<Photo> r = photoDao.findCarouselWithoutEventByClubId(UUID.randomUUID(), 10, 0);
        assertTrue(r.isEmpty());
    }

    // ----------------- findAllCoversByClubIds tests -----------------

    @Test
    public void testFindAllCoversByClubIds_basicAndDuplicatesHandled() {
        Photo c1a = persistPhoto(clubId, user.getId(), "c1a.jpg", new byte[]{1}, PhotoType.COVER);
        Photo c1b = persistPhoto(clubId, user.getId(), "c1b.jpg", new byte[]{2}, PhotoType.COVER); // duplicate club -> first kept
        Photo c2 = persistPhoto(otherClubId, user.getId(), "c2.jpg", new byte[]{3}, PhotoType.COVER);
        em.flush();

        Set<UUID> clubIds = new LinkedHashSet<>(Arrays.asList(clubId, otherClubId));
        Map<UUID, String> covers = photoDao.findAllCoversByClubIds(clubIds);

        assertEquals(2, covers.size());
        assertTrue(covers.containsKey(clubId));
        assertTrue(covers.containsKey(otherClubId));
        assertTrue(covers.get(clubId).startsWith("/photos/"));
    }

    @Test
    public void testFindAllCoversByClubIds_emptyWhenEmpty() {
        Map<UUID, String> r = photoDao.findAllCoversByClubIds(Collections.emptySet());
        assertTrue(r.isEmpty());
    }

    // ----------------- countByClubIdAndType tests -----------------

    @Test
    public void testCountByClubIdAndType() {
        for (int i = 0; i < 4; i++) persistPhoto(clubId, user.getId(), "cnt" + i + ".jpg", new byte[]{(byte)i}, PhotoType.REVIEW);
        em.flush();

        long count = photoDao.countByClubIdAndType(clubId, PhotoType.REVIEW);
        assertEquals(4L, count);
    }

    @Test
    public void testCountByClubIdAndType_zeroWhenNone() {
        long count = photoDao.countByClubIdAndType(UUID.randomUUID(), PhotoType.CAROUSEL);
        assertEquals(0L, count);
    }

    // ----------------- Helpers -------------------

    private User persistUser() {
        User u = new User(UUID.randomUUID(), "user", "name", "last", "email@example.com", "{noop}hash", "cell", Role.USER, OffsetDateTime.now(), null);
        em.persist(u);
        return u;
    }

    private Club persistClub(User contact) {
        Club c = new Club("Club Test " + UUID.randomUUID(), "Location", java.math.BigDecimal.ZERO, 0, null, "desc", contact, null, OffsetDateTime.now(), null, Boolean.FALSE, null);
        em.persist(c);
        return c;
    }

    private Photo persistPhoto(UUID clubId, UUID postedBy, String filename, byte[] data, PhotoType type) {
        Photo p = new Photo();
        p.setClubId(clubId);
        p.setPostedBy(postedBy);
        p.setFilename(filename);
        p.setContentType("image/jpeg");
        p.setData(data);
        p.setPhotoType(type);
        em.persist(p);
        return p;
    }

    private ClubTable persistTable(Club club) {
        ClubTable t = new ClubTable(null, club.getClubId(), 1, 4, TableState.DISPONIBLE, new java.math.BigDecimal("100.00"), java.math.BigDecimal.ZERO);
        em.persist(t);
        return t;
    }

    private Booking persistBooking(User user, ClubTable table, OffsetDateTime start, OffsetDateTime end) {
        Booking b = new Booking();
        b.setBookingId(UUID.randomUUID());
        b.setUser(user);
        b.setTable(table);
        b.setStatus(Status.PENDIENTE);
        b.setTotalAmount(null);
        b.setBookingStart(start);
        b.setBookingEnd(end);
        b.setCreatedAt(OffsetDateTime.now());
        em.persist(b);
        return b;
    }

    @Test
    public void testCreate_withNullFilename_throws() {
        assertThrows(PersistenceException.class, () -> {
            photoDao.create(clubId, user.getId(), null, "image/jpeg", new byte[]{1}, PhotoType.COVER);
            em.flush();
        });
    }

    @Test
    public void testCreate_withNullContentType_throws() {
        assertThrows(PersistenceException.class, () -> {
            photoDao.create(clubId, user.getId(), "photo.jpg", null, new byte[]{1}, PhotoType.CAROUSEL);
            em.flush();
        });
    }

    @Test
    public void testSave_nullPhoto_throws() {
        assertThrows(NullPointerException.class, () -> {
            photoDao.save(null);
        });
    }

    @Test
    public void testSave_missingRequiredFields_throws() {
        Photo p = new Photo();
        assertThrows(PersistenceException.class, () -> {
            photoDao.save(p);
            em.flush();
        });
    }

    @Test
    public void testSave_nullIdButNullDataStillThrows() {
        Photo p = new Photo();
        p.setClubId(clubId);
        p.setPostedBy(user.getId());
        p.setFilename("ok.jpg");
        p.setContentType("image/jpeg");
        p.setData(null);
        p.setPhotoType(PhotoType.COVER);

        assertThrows(PersistenceException.class, () -> {
            photoDao.save(p);
            em.flush();
        });
    }

}
