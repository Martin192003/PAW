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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ClubTablesJpaDaoTest {

    @Autowired
    private ClubTablesJpaDao clubTablesDao;

    @PersistenceContext
    private EntityManager em;

    private UUID clubId;
    private UUID tableId;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM ClubTable t").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        User admin = persistUser("admin", "Admin", "Last", "admin@email.com",
                "{noop}pass", "123456", Role.USER);
        Club club = persistClub("Club", "Desc", "Loc",
                Price.MEDIO, admin, MusicalGenre.ROCK, Zone.ZONA_CABA, false, null);
        clubId = club.getClubId();

        ClubTable t = clubTablesDao.create(
                1, 4, clubId, TableState.DISPONIBLE,
                new BigDecimal("100"), new BigDecimal("10")
        );
        tableId = t.getTableId();
        em.flush();
    }

    private User persistUser(String username, String name, String surname, String email,
                             String passwordHash, String cellphone, Role role) {
        User u = new User(UUID.randomUUID(), username, name, surname, email,
                passwordHash, cellphone, role, OffsetDateTime.now());
        em.persist(u);
        return u;
    }

    private Club persistClub(String name, String desc, String loc, Price price,
                             User contact, MusicalGenre genre, Zone zone, Boolean isBar, String cbu) {
        Club c = new Club(
                name, loc,  null,  null,
                price, desc, contact, genre, null, zone, isBar, cbu
        );
        em.persist(c);
        return c;
    }

    private long countTables() {
        return em.createQuery("select count(t) from ClubTable t", Long.class)
                .getSingleResult();
    }

    // ---------- Tests ----------

    @Test
    public void testCreateTable() {
        // Nuevo club para no mezclar con el del setUp
        User admin2 = persistUser("admin2", "Name2", "Last2", "email2@email.com",
                "{noop}pass2", "654321", Role.USER);
        Club club2 = persistClub("Club2", "Desc2", "Loc2",
                Price.MEDIO, admin2, MusicalGenre.ROCK, Zone.ZONA_CABA, false, null);

        int number = 1;
        int capacity = 4;
        TableState state = TableState.DISPONIBLE;
        BigDecimal price = new BigDecimal("100");
        BigDecimal discount = new BigDecimal("10");

        ClubTable table = clubTablesDao.create(number, capacity, club2.getClubId(), state, price, discount);

        assertNotNull(table);
        assertEquals(number, table.getTableNumber());
        assertEquals(capacity, table.getCapacity());
        assertEquals(club2.getClubId(), table.getClubId());
        assertEquals(state, table.getTableState());
        assertEquals(0, price.compareTo(table.getPrice()));
        assertEquals(0, discount.compareTo(table.getDiscount()));
        assertEquals(2L, countTables()); // la del setUp + esta
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        Optional<ClubTable> result = clubTablesDao.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        ClubTable result = clubTablesDao.update(
                UUID.randomUUID(), UUID.randomUUID(), 2, 6,
                TableState.RESERVADA, new BigDecimal("200"), new BigDecimal("20")
        );
        assertNull(result);
    }

    @Test
    public void testDeleteReturnsFalseIfNotFound() {
        boolean deleted = clubTablesDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }

    @Test
    public void testFindByClubReturnsEmptyList() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        List<ClubTable> result = clubTablesDao.findByClub(club);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateSuccess() {
        ClubTable updated = clubTablesDao.update(
                tableId, clubId, 2, 6,
                TableState.RESERVADA, new BigDecimal("200"), new BigDecimal("20")
        );

        assertNotNull(updated);
        assertEquals(2, updated.getTableNumber());
        assertEquals(6, updated.getCapacity());
        assertEquals(TableState.RESERVADA, updated.getTableState());
        assertEquals(0, new BigDecimal("200").compareTo(updated.getPrice()));
        assertEquals(0, new BigDecimal("20").compareTo(updated.getDiscount()));
    }

    @Test
    public void testDeleteSuccess() {
        boolean deleted = clubTablesDao.delete(tableId);
        assertTrue(deleted);
        assertEquals(0L, countTables());
    }

    @Test
    public void testFindByIdSuccess() {
        Optional<ClubTable> result = clubTablesDao.findById(tableId);
        assertTrue(result.isPresent());
        assertEquals(tableId, result.get().getTableId());
    }

    @Test
    public void testFindAllWithTables() {
        ClubTable t2 = clubTablesDao.create(
                2, 2, clubId, TableState.DISPONIBLE,
                new BigDecimal("50"), new BigDecimal("5")
        );
        em.flush();

        List<ClubTable> tables = clubTablesDao.findAll();
        assertEquals(2, tables.size());
        assertTrue(tables.stream().anyMatch(t -> t.getTableId().equals(t2.getTableId())));
    }

    @Test
    public void testFindByClubWithTables() {
        Club clubProxy = new Club(clubId, null, null, null, null, null, null, null, null, null, null, null, null);
        List<ClubTable> tables = clubTablesDao.findByClub(clubProxy);

        assertEquals(1, tables.size());
        assertEquals(tableId, tables.getFirst().getTableId());
    }

    @Test
    public void testFindAvailableByClubAndRange() {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusHours(2);

        List<ClubTable> available = clubTablesDao.findAvailableByClubAndRange(clubId, from, to);

        assertEquals(1, available.size());
        assertEquals(tableId, available.getFirst().getTableId());
    }
}
