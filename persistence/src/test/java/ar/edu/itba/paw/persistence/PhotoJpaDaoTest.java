package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
@Transactional
public class PhotoJpaDaoTest {

    @Autowired
    private DataSource ds;
    @Autowired
    private PhotoJpaDao photoDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "photos");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    private void insertClub(UUID clubId) {
        jdbcTemplate.update(
            "INSERT INTO clubs (club_id, club_name, location, is_bar, created_at) VALUES (?, ?, ?, ?, NOW())",
            clubId, "Test Club", "Test Location", false
        );
    }

    private void insertUser(UUID userId) {
        String email = userId.toString() + "@example.com";
        jdbcTemplate.update(
            "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
            userId, "Test", "User", email, "hash", "123456", "user"
        );
    }

    @Test
    public void testCreateAndFindByIdWithData() {
        UUID clubId = UUID.randomUUID();
        insertClub(clubId);
        UUID postedBy = UUID.randomUUID();
        insertUser(postedBy);
        String filename = "test.jpg";
        String contentType = "image/jpeg";
        byte[] data = new byte[]{1,2,3,4,5};
        UUID photoId = photoDao.create(clubId, postedBy, filename, contentType, data);
        assertNotNull(photoId);
        Optional<Photo> photoOpt = photoDao.findByIdWithData(photoId);
        assertTrue(photoOpt.isPresent());
        Photo photo = photoOpt.get();
        assertEquals(clubId, photo.getClubId());
        assertEquals(postedBy, photo.getPostedBy());
        assertEquals(filename, photo.getFilename());
        assertEquals(contentType, photo.getContentType());
        assertArrayEquals(data, photo.getData());
    }

    @Test
    public void testFindByClub() {
        UUID clubId = UUID.randomUUID();
        insertClub(clubId);
        UUID postedBy = UUID.randomUUID();
        insertUser(postedBy);
        String filename = "test2.jpg";
        String contentType = "image/jpeg";
        byte[] data = new byte[]{10,20,30};
        photoDao.create(clubId, postedBy, filename, contentType, data);
        List<Photo> photos = photoDao.findByClub(clubId, 10, 0);
        assertEquals(1, photos.size());
        Photo photo = photos.get(0);
        assertEquals(clubId, photo.getClubId());
        assertEquals(filename, photo.getFilename());
    }

    @Test
    public void testFindById() {
        UUID clubId = UUID.randomUUID();
        insertClub(clubId);
        UUID postedBy = UUID.randomUUID();
        insertUser(postedBy);
        String filename = "test3.jpg";
        String contentType = "image/png";
        byte[] data = new byte[]{7,8,9};
        UUID photoId = photoDao.create(clubId, postedBy, filename, contentType, data);
        Optional<Photo> photoOpt = photoDao.findById(photoId);
        assertTrue(photoOpt.isPresent());
        Photo photo = photoOpt.get();
        assertEquals(clubId, photo.getClubId());
        assertEquals(filename, photo.getFilename());
        assertNull(photo.getData()); // findById no trae los datos binarios
    }

    @Test
    public void testFindByBooking() {
        // Simular booking y foto asociada
        UUID clubId = UUID.randomUUID();
        insertClub(clubId);
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        String filename = "booking.jpg";
        String contentType = "image/jpeg";
        byte[] data = new byte[]{5,6,7};
        UUID photoId = photoDao.create(clubId, userId, filename, contentType, data);
        // Crear mesa válida
        UUID tableId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
            tableId, clubId, 1, 4, "disponible", 100, 0
        );
        // Insertar booking con ese userId y la mesa creada
        UUID bookingId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO bookings (booking_id, user_id, table_id, status, total_amount, booking_start, booking_end) VALUES (?, ?, ?, ?, ?, NOW(), DATEADD('HOUR', 1, NOW()))",
            bookingId, userId, tableId, "pendiente", 100
        );
        Optional<Photo> found = photoDao.findByBooking(bookingId);
        assertTrue(found.isPresent());
        assertEquals(photoId, found.get().getPhotoId());
    }

    @Test
    public void testDeleteSuccess() {
        UUID clubId = UUID.randomUUID();
        insertClub(clubId);
        UUID postedBy = UUID.randomUUID();
        insertUser(postedBy);
        String filename = "delete.jpg";
        String contentType = "image/jpeg";
        byte[] data = new byte[]{1,2};
        UUID photoId = photoDao.create(clubId, postedBy, filename, contentType, data);
        boolean deleted = photoDao.delete(photoId);
        assertTrue(deleted);
        Optional<Photo> found = photoDao.findById(photoId);
        assertFalse(found.isPresent());
    }

    @Test
    public void testDeleteNotFound() {
        UUID randomId = UUID.randomUUID();
        boolean deleted = photoDao.delete(randomId);
        assertFalse(deleted);
    }

    @Test
    public void testFindByIdNotFound() {
        UUID randomId = UUID.randomUUID();
        Optional<Photo> found = photoDao.findById(randomId);
        assertFalse(found.isPresent());
    }

    @Test
    public void testFindByIdWithDataNotFound() {
        UUID randomId = UUID.randomUUID();
        Optional<Photo> found = photoDao.findByIdWithData(randomId);
        assertFalse(found.isPresent());
    }
}
