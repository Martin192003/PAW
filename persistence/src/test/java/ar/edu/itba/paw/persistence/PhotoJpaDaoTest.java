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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class PhotoJpaDaoTest {

    @Autowired private PhotoJpaDao photoDao;
    @PersistenceContext private EntityManager em;

    /*@Before
    public void setUp() {
        // Limpieza simple (cada test igual hace rollback por @Transactional)
        em.createQuery("DELETE FROM Photo p").executeUpdate();
        em.createQuery("DELETE FROM Booking b").executeUpdate();
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();
    }

    // -------- Helpers de persistencia JPA --------

    private User persistUser(String username) {
        User u = new User(
                UUID.randomUUID(), username, "Name", "Last",
                username + "@example.com", "{noop}hash", "123456",
                Role.USER, OffsetDateTime.now()
        );
        em.persist(u);
        return u;
    }

    private Club persistClub(String name, User contact) {
        // isBar = false para evitar NOT NULL (o rely en @PrePersist si lo agregaste)
        Club c = new Club(name, "Loc", null, null, Price.MEDIO,
                "Desc", contact, MusicalGenre.ROCK, null, Zone.ZONA_CABA, false, null);
        em.persist(c);
        return c;
    }

    private ClubTable persistTable(Club club) {
        ClubTable t = new ClubTable(
                null, club.getClubId(), 1, 4, TableState.DISPONIBLE,
                new BigDecimal("100"), new BigDecimal("0")
        );
        em.persist(t); // @PrePersist genera UUID si es null
        return t;
    }

    private Booking persistBooking(User user, ClubTable table) {
        Booking b = new Booking(
                UUID.randomUUID(), user, table,
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(1),
                Status.PENDIENTE, OffsetDateTime.now(), new BigDecimal("100")
        );
        em.persist(b);
        return b;
    }

    private long countPhotos() {
        return em.createQuery("select count(p) from Photo p", Long.class).getSingleResult();
    }

    // -------------- Tests --------------

    @Test
    public void testCreateAndFindByIdWithData() {
        User u = persistUser("u1");
        Club c = persistClub("Club Test", u);

        String filename = "test.jpg";
        String contentType = "image/jpeg";
        byte[] data = new byte[]{1,2,3,4,5};

        UUID photoId = photoDao.create(c.getClubId(), u.getId(), filename, contentType, data, PhotoType.CAROUSEL);
        assertNotNull(photoId);

        Optional<Photo> photoOpt = photoDao.findByIdWithData(photoId);
        assertTrue(photoOpt.isPresent());

        Photo photo = photoOpt.get();
        assertEquals(c.getClubId(), photo.getClubId());
        assertEquals(u.getId(), photo.getPostedBy());
        assertEquals(filename, photo.getFilename());
        assertEquals(contentType, photo.getContentType());
        assertArrayEquals(data, photo.getData());
        assertEquals(1L, countPhotos());
    }

    @Test
    public void testFindByClub() {
        User u = persistUser("u2");
        Club c = persistClub("Club X", u);

        UUID id = photoDao.create(c.getClubId(), u.getId(), "test2.jpg", "image/jpeg", new byte[]{10,20,30}, PhotoType.CAROUSEL);
        assertNotNull(id);

        List<Photo> photos = photoDao.findByClub(c.getClubId(), 10, 0);
        assertEquals(1, photos.size());
        assertEquals(c.getClubId(), photos.getFirst().getClubId());
        assertEquals("test2.jpg", photos.getFirst().getFilename());
        assertNull(photos.getFirst().getData()); // sin data por performance
    }

    @Test
    public void testFindById() {
        User u = persistUser("u3");
        Club c = persistClub("Club Y", u);

        UUID photoId = photoDao.create(c.getClubId(), u.getId(), "test3.png", "image/png", new byte[]{7,8,9}, PhotoType.CAROUSEL);
        Optional<Photo> photoOpt = photoDao.findById(photoId);

        assertTrue(photoOpt.isPresent());
        Photo photo = photoOpt.get();
        assertEquals(c.getClubId(), photo.getClubId());
        assertEquals("test3.png", photo.getFilename());
        assertNull(photo.getData()); // findById no trae binarios
    }

    @Test
    public void testFindByBooking() {
        // Booking del user en una mesa del club; la foto es (club_id, posted_by=user_id)
        User u = persistUser("u4");
        Club c = persistClub("Club Z", u);
        ClubTable t = persistTable(c);
        Booking b = persistBooking(u, t);

        UUID photoId = photoDao.create(c.getClubId(), u.getId(), "booking.jpg", "image/jpeg", new byte[]{5,6,7}, PhotoType.CAROUSEL);

        Optional<Photo> found = photoDao.findByBooking(b.getBookingId());
        assertTrue(found.isPresent());
        assertEquals(photoId, found.get().getPhotoId());
    }

    @Test
    public void testDeleteSuccess() {
        User u = persistUser("u5");
        Club c = persistClub("Club Del", u);
        UUID photoId = photoDao.create(c.getClubId(), u.getId(), "delete.jpg", "image/jpeg", new byte[]{1,2}, PhotoType.CAROUSEL);

        boolean deleted = photoDao.delete(photoId);
        assertTrue(deleted);

        Optional<Photo> found = photoDao.findById(photoId);
        assertFalse(found.isPresent());
        assertEquals(0L, countPhotos());
    }

    @Test
    public void testDeleteNotFound() {
        boolean deleted = photoDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }

    @Test
    public void testFindByIdNotFound() {
        assertFalse(photoDao.findById(UUID.randomUUID()).isPresent());
    }

    @Test
    public void testFindByIdWithDataNotFound() {
        assertFalse(photoDao.findByIdWithData(UUID.randomUUID()).isPresent());
    }
}*/
