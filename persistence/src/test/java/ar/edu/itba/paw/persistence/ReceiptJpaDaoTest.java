/*package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
@Transactional
public class ReceiptJpaDaoTest {

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private ReceiptJpaDao receiptDao;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Receipt").executeUpdate();
        em.createQuery("DELETE FROM Booking").executeUpdate();
        em.createQuery("DELETE FROM ClubTable").executeUpdate();
        em.createQuery("DELETE FROM Club").executeUpdate();
        em.createQuery("DELETE FROM User").executeUpdate();
    }

    private User insertUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);
        user.setUserName("Test");
        user.setName("TestName");
        user.setSurname("User");
        user.setEmail(userId.toString() + "@example.com");
        user.setPasswordHash("hash");
        user.setCellphone("123456");
        user.setRole(Role.USER);
        user.setCreatedAt(OffsetDateTime.now());
        em.persist(user);
        return user;
    }

    private Club insertClub(UUID clubId, User contact) {
        Club club = em.find(Club.class, clubId);
        if (club != null) {
            return club;
        }
        club = new Club();
        club.setClubId(clubId);
        club.setClubName("Club Test");
        club.setLocation("Location");
        club.setRating(BigDecimal.ZERO);
        club.setQuantityRatings(1);
        club.setPriceRange(Price.MEDIO);
        club.setDescription("desc");
        club.setContactInfo(em.merge(contact));
        club.setMusicGenre(MusicalGenre.POP);
        club.setZone(Zone.ZONA_CABA);
        club.setIsBar(false);
        club.setCreatedAt(OffsetDateTime.now());
        club.setAdmins(new HashSet<>());
        return em.merge(club);
    }

    private ClubTable insertTable(UUID tableId, Club club) {
        ClubTable table = new ClubTable();
        table.setTableId(tableId);
        table.setClubId(club.getClubId());
        table.setTableNumber(1);
        table.setCapacity(4);
        table.setTableState(TableState.DISPONIBLE);
        table.setPrice(BigDecimal.valueOf(100.0));
        table.setDiscount(BigDecimal.valueOf(0.0));
        em.persist(table);
        return table;
    }

    private Booking insertBooking(UUID bookingId, User user, ClubTable table) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUser(user);
        booking.setTable(table);
        booking.setBookingStart(OffsetDateTime.now());
        booking.setBookingEnd(OffsetDateTime.now().plusHours(1));
        booking.setStatus(Status.CONFIRMADA);
        booking.setCreatedAt(OffsetDateTime.now());
        booking.setTotalAmount(BigDecimal.valueOf(100.0));
        em.persist(booking);
        return booking;
    }

    @Test
    public void testCreateAndFindByIdWithData() {
        UUID userId = UUID.randomUUID();
        User user = insertUser(userId);
        UUID clubId = UUID.randomUUID();
        Club club = insertClub(clubId, user);
        UUID tableId = UUID.randomUUID();
        ClubTable table = insertTable(tableId, club);
        UUID bookingId = UUID.randomUUID();
        Booking booking = insertBooking(bookingId, user, table);
        String filename = "receipt.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{1,2,3,4,5};
        UUID receiptId = receiptDao.create(bookingId, userId, filename, contentType, data);
        assertNotNull(receiptId);
        Optional<Receipt> receiptOpt = receiptDao.findByIdWithData(receiptId);
        assertTrue(receiptOpt.isPresent());
        Receipt receipt = receiptOpt.get();
        assertEquals(bookingId, receipt.getBookingId());
        assertEquals(userId, receipt.getUploadedBy());
        assertEquals(filename, receipt.getFilename());
        assertEquals(contentType, receipt.getContentType());
        assertArrayEquals(data, receipt.getData());
    }

    @Test
    public void testFindByBooking() {
        UUID userId = UUID.randomUUID();
        User user = insertUser(userId);
        UUID clubId = UUID.randomUUID();
        Club club = insertClub(clubId, user);
        UUID tableId = UUID.randomUUID();
        ClubTable table = insertTable(tableId, club);
        UUID bookingId = UUID.randomUUID();
        Booking booking = insertBooking(bookingId, user, table);
        String filename = "receipt2.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{10,20,30};
        receiptDao.create(bookingId, userId, filename, contentType, data);
        Optional<Receipt> receiptOpt = receiptDao.findByBooking(bookingId);
        assertTrue(receiptOpt.isPresent());
        Receipt receipt = receiptOpt.get();
        assertEquals(bookingId, receipt.getBookingId());
        assertEquals(filename, receipt.getFilename());
    }

    @Test
    public void testFindById() {
        UUID userId = UUID.randomUUID();
        User user = insertUser(userId);
        UUID clubId = UUID.randomUUID();
        Club club = insertClub(clubId, user);
        UUID tableId = UUID.randomUUID();
        ClubTable table = insertTable(tableId, club);
        UUID bookingId = UUID.randomUUID();
        Booking booking = insertBooking(bookingId, user, table);
        String filename = "receipt3.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{7,8,9};
        UUID receiptId = receiptDao.create(bookingId, userId, filename, contentType, data);
        Optional<Receipt> receiptOpt = receiptDao.findById(receiptId);
        assertTrue(receiptOpt.isPresent());
        Receipt receipt = receiptOpt.get();
        assertEquals(bookingId, receipt.getBookingId());
        assertEquals(filename, receipt.getFilename());
        assertNotNull(receipt.getData());
    }

    @Test
    public void testDeleteByBooking() {
        UUID userId = UUID.randomUUID();
        User user = insertUser(userId);
        UUID clubId = UUID.randomUUID();
        Club club = insertClub(clubId, user);
        UUID tableId = UUID.randomUUID();
        ClubTable table = insertTable(tableId, club);
        UUID bookingId = UUID.randomUUID();
        Booking booking = insertBooking(bookingId, user, table);
        String filename = "receipt4.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{11,12,13};
        receiptDao.create(bookingId, userId, filename, contentType, data);
        boolean deleted = receiptDao.deleteByBooking(bookingId);
        assertTrue(deleted);
        assertEquals(0, ((Number)em.createQuery("SELECT COUNT(r) FROM Receipt r").getSingleResult()).intValue());
    }

    @Test
    public void testDeleteById() {
        UUID userId = UUID.randomUUID();
        User user = insertUser(userId);
        UUID clubId = UUID.randomUUID();
        Club club = insertClub(clubId, user);
        UUID tableId = UUID.randomUUID();
        ClubTable table = insertTable(tableId, club);
        UUID bookingId = UUID.randomUUID();
        Booking booking = insertBooking(bookingId, user, table);
        String filename = "receipt5.pdf";
        String contentType = "application/pdf";
        byte[] data = new byte[]{21,22,23};
        UUID receiptId = receiptDao.create(bookingId, userId, filename, contentType, data);
        boolean deleted = receiptDao.delete(receiptId);
        assertTrue(deleted);
        assertEquals(0, ((Number)em.createQuery("SELECT COUNT(r) FROM Receipt r").getSingleResult()).intValue());
    }
}*/
