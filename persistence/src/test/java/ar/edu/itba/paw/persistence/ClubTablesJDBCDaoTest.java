package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Table;
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
    private ClubTablesJDBCDao clubTablesDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "club_tables");
    }

    @Test
    public void testCreateTable() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (user_id, user_name, surname, email, password_hash, cellphone, role) VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, "admin", "last", "email@email.com", "pass", "123456", "user"
        );

        UUID clubId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO clubs (club_id, club_name, location, rating, quantity_ratings, price_range, description, contact_info, music_genre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                clubId, "Club", "Loc", 1.0, 0, "medio", "Desc", userId, "Rock"
        );

        int number = 1;
        int capacity = 4;
        TableState state = TableState.DISPONIBLE;
        BigDecimal price = BigDecimal.valueOf(100);
        BigDecimal discount = BigDecimal.valueOf(10);

        Table table = clubTablesDao.create(number, capacity, clubId, state, price, discount);

        assertNotNull(table);
        assertEquals(number, table.getTableNumber());
        assertEquals(capacity, table.getCapacity());
        assertEquals(clubId, table.getClubId());
        assertEquals(state, table.getTableState());
        assertEquals(price, table.getPrice());
        assertEquals(discount, table.getDiscount());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "club_tables"));
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        Optional<Table> result = clubTablesDao.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAllReturnsEmptyList() {
        List<Table> result = clubTablesDao.findAll();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        Table result = clubTablesDao.update(UUID.randomUUID(), UUID.randomUUID(), 2, 6, TableState.RESERVADA, BigDecimal.valueOf(200), BigDecimal.valueOf(20));
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
        List<Table> result = clubTablesDao.findByClub(club);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}