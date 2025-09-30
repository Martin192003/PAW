package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
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

import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class ClubJDBCDaoTest {

    private static final String NAME = "Club Test";
    private static final String DESCRIPTION = "Descripción";
    private static final String LOCATION = "Ubicación";
    private static final Price PRICE_RANGE = Price.MEDIO;
    private static final MusicalGenre MUSIC_GENRE = MusicalGenre.ROCK;

    @Autowired
    private DataSource ds;
    @Autowired
    private ClubJDBCDao clubDao;
    @Autowired
    private UserJDBCDao userDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    @Test
    public void testCreate() {
        User contact = userDao.create("user","name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create(NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, null);
        assertNotNull(club);
        assertEquals(NAME, club.getClubName());
        assertEquals(DESCRIPTION, club.getDescription());
        assertEquals(LOCATION, club.getLocation());
        assertEquals(PRICE_RANGE, club.getPriceRange());
        assertEquals(MUSIC_GENRE, club.getMusicGenre());
        assertEquals(contact.getId(), club.getContactInfo().getId());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "clubs"));
    }
}