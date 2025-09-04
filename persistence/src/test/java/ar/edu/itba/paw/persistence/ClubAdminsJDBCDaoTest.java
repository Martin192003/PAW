package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
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
import java.util.UUID;
import java.util.Optional;


import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class ClubAdminsJDBCDaoTest {

    @Autowired
    private DataSource ds;
    @Autowired
    private ClubAdminsJDBCDao clubAdminsDao;
    @Autowired
    private ClubJDBCDao clubDao;
    @Autowired
    private UserJDBCDao userDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "club_admins");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    @Test
    public void testAddAdmin() {
        User admin = userDao.create("admin", "pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create("Club", "Desc", "Loc", "medio", admin, "Rock");
        boolean added = clubAdminsDao.addAdmin(club, admin);
        assertTrue(added);
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "club_admins"));
    }

    @Test
    public void testExistsReturnsFalse() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        assertFalse(clubAdminsDao.exists(club, admin));
    }

    @Test
    public void testFindAdminsReturnsEmptyList() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        List<User> admins = clubAdminsDao.findAdmins(club);
        assertNotNull(admins);
        assertTrue(admins.isEmpty());
    }

    @Test
    public void testFindClubsByAdminReturnsEmptyList() {
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        List<Club> clubs = clubAdminsDao.findClubsByAdmin(admin);
        assertNotNull(clubs);
        assertTrue(clubs.isEmpty());
    }

    @Test
    public void testRemoveAdminReturnsFalseIfNotFound() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        assertFalse(clubAdminsDao.removeAdmin(club, admin));
    }
}