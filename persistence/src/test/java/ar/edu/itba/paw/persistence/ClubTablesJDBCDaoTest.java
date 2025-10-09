package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class ClubTablesJDBCDaoTest {

    @Autowired
    private DataSource ds;
    @Autowired
    private ClubTablesJpaDao clubTablesDao;
    private JdbcTemplate jdbcTemplate;

    private UUID clubId;
    private UUID tableId;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "club_tables");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
        // Crear usuario y club para los tests
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, "admin", "last", "email@email.com", "pass", "123456", "user"
        );
        clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club", "Loc", 1.0, 0, "medio", "Desc", userId, "Rock"
        );
        // Crear mesa para los tests
        tableId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableId, clubId, 1, 4, "disponible", 100, 10
        );
    }

    @Test
    public void testCreateTable() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, "admin2", "last2", "email2@email.com", "pass2", "654321", "user"
        );

        UUID clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club2", "Loc2", 1.0, 0, "medio", "Desc2", userId, "Rock"
        );

        int number = 1;
        int capacity = 4;
        TableState state = TableState.DISPONIBLE;
        BigDecimal price = BigDecimal.valueOf(100);
        BigDecimal discount = BigDecimal.valueOf(10);

        ClubTable table = clubTablesDao.create(number, capacity, clubId, state, price, discount);

        assertNotNull(table);
        assertEquals(number, table.getTableNumber());
        assertEquals(capacity, table.getCapacity());
        assertEquals(clubId, table.getClubId());
        assertEquals(state, table.getTableState());
        assertEquals(price, table.getPrice());
        assertEquals(discount, table.getDiscount());
        assertEquals(2, JdbcTestUtils.countRowsInTable(jdbcTemplate, "club_tables"));
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        Optional<ClubTable> result = clubTablesDao.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        ClubTable result = clubTablesDao.update(UUID.randomUUID(), UUID.randomUUID(), 2, 6, TableState.RESERVADA, BigDecimal.valueOf(200), BigDecimal.valueOf(20));
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
        ClubTable updated = clubTablesDao.update(tableId, clubId, 2, 6, TableState.RESERVADA, BigDecimal.valueOf(200), BigDecimal.valueOf(20));
        assertNotNull(updated);
        assertEquals(2, updated.getTableNumber());
        assertEquals(6, updated.getCapacity());
        assertEquals(TableState.RESERVADA, updated.getTableState());
        assertTrue(BigDecimal.valueOf(200).compareTo(updated.getPrice()) == 0);
        assertTrue(BigDecimal.valueOf(20).compareTo(updated.getDiscount()) == 0);
    }

    @Test
    public void testDeleteSuccess() {
        boolean deleted = clubTablesDao.delete(tableId);
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "club_tables"));
    }

    @Test
    public void testFindByIdSuccess() {
        Optional<ClubTable> result = clubTablesDao.findById(tableId);
        assertTrue(result.isPresent());
        assertEquals(tableId, result.get().getTableId());
    }

    @Test
    public void testFindAllWithTables() {
        // Agregar otra mesa
        UUID tableId2 = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO club_tables (table_id, club_id, table_number, capacity, table_state, price, discount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                tableId2, clubId, 2, 2, "disponible", 50, 5
        );
        List<ClubTable> tables = clubTablesDao.findAll();
        assertEquals(2, tables.size());
    }

    @Test
    public void testFindByClubWithTables() {
        List<ClubTable> tables = clubTablesDao.findByClub(new Club(clubId, null, null, null, null, null, null, null, null, null, null, null));
        assertEquals(1, tables.size());
        assertEquals(tableId, tables.get(0).getTableId());
    }

    @Test
    public void testFindAvailableByClubAndRange() {
        // No hay bookings, la mesa debe estar disponible
        java.time.OffsetDateTime from = java.time.OffsetDateTime.now();
        java.time.OffsetDateTime to = from.plusHours(2);
        List<ClubTable> available = clubTablesDao.findAvailableByClubAndRange(clubId, from, to);
        assertEquals(1, available.size());
        assertEquals(tableId, available.get(0).getTableId());
    }
}
