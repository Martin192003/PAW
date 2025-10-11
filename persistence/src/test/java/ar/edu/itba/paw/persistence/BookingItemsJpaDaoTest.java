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
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class BookingItemsJpaDaoTest {

    @Autowired
    private BookingItemsJpaDao bookingItemsDao;

    @PersistenceContext
    private EntityManager em;

    @Before
    public void clean() {
        // Con @Transactional cada test hace rollback, pero limpiamos por si tu config cambia.
        em.createQuery("DELETE FROM BookingItem bi").executeUpdate();
        em.flush();
    }

    // ---------- Helpers de armado del grafo ----------
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
                null, // Price (opcional si tu columna permite null)
                "desc",
                contact,
                null, // MusicalGenre
                OffsetDateTime.now(),
                null, // Zone
                Boolean.FALSE
        );
        em.persist(c);
        return c;
    }

    private ClubTable persistTable(Club club) {
        ClubTable t = new ClubTable(
                null,                    // UUID: se setea en @PrePersist si null
                club.getClubId(),        // FK por UUID (no ManyToOne)
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
                UUID.randomUUID(),
                u,
                t,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                Status.CONFIRMADA,
                OffsetDateTime.now(),
                new BigDecimal("100.00")
        );
        em.persist(b);
        return b;
    }

    private Product persistProduct(Club club) {
        Product p = new Product(
                null,
                club,
                "Producto Test",
                "desc",
                new BigDecimal("100.00"),
                ProductType.ALCOHOLICA,
                Boolean.TRUE,
                OffsetDateTime.now()
        );
        em.persist(p);
        return p;
    }

    private long countBookingItems() {
        return em.createQuery("select count(bi) from BookingItem bi", Long.class)
                .getSingleResult();
    }

    // ---------------- TEST ----------------

    @Test
    public void testCreate() {
        User user = persistUser();
        Club club = persistClub(user);
        ClubTable table = persistTable(club);
        Booking booking = persistBooking(user, table);
        Product product = persistProduct(club);
        em.flush();

        int quantity = 2;
        BigDecimal unitPrice = new BigDecimal("100.00");
        BigDecimal expectedLineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Act
        BookingItem item = bookingItemsDao.create(booking, product, quantity, unitPrice);

        // Assert
        assertNotNull(item);
        assertEquals(booking.getBookingId(), item.getBooking().getBookingId());
        assertEquals(product.getProductId(), item.getProduct().getProductId());
        assertEquals(quantity, item.getQuantity().intValue());
        assertEquals(unitPrice, item.getUnitPrice());
        assertEquals(expectedLineTotal, item.getLineTotal());
        assertEquals(1L, countBookingItems());
    }
}
