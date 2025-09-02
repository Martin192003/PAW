package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class PhotoJDBCDaoTest {

    private static final String URL = "http://test.com/photo.jpg";

    @Autowired
    private DataSource ds;
    @Autowired
    private PhotoJDBCDao photoDao;
    @Autowired
    private ClubDao clubDao;
    @Autowired
    private UserDao userDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "photos");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    @Test
    public void testCreatePhoto() {
        User user = userDao.create("user", "pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create("Club", "Desc", "Loc", "medio", user, "Rock"); // ← "medio" en minúsculas
        Photo photo = photoDao.create(URL, club, user);

        assertNotNull(photo);
        assertEquals(URL, photo.getUrl());
        assertEquals(club.getClubId(), photo.getClub().getClubId());
        assertEquals(user.getId(), photo.getPostedBy().getId());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "photos"));
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        Optional<Photo> result = photoDao.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByClubReturnsEmptyList() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        List<Photo> result = photoDao.findByClub(club);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByUserReturnsEmptyList() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        List<Photo> result = photoDao.findByUser(user);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        Photo result = photoDao.update(UUID.randomUUID(), "http://newurl.com/photo.jpg");
        assertNull(result);
    }

    @Test
    public void testDeleteReturnsFalseIfNotFound() {
        boolean deleted = photoDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }
}