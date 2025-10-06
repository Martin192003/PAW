package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import javax.sql.DataSource;
import ar.edu.itba.paw.model.User;
import org.junit.runner.RunWith;

import java.util.Optional;
import java.util.UUID;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class UserJDBCDaoTest {
    private static final String PASSWORD = "Password";
    private static final String USERNAME = "Username";
    private static final String EMAIL = "Email";
    private static final String NAME = "Name";
    private static final String LASTNAME = "LastName";
    private static final String CELLPHONE = "cellphone";

    @Autowired
    private DataSource ds;
    @Autowired
    private UserJDBCDao userDao;
    private JdbcTemplate jdbcTemplate;
    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }
    @Test
    public void testCreate() {
        final User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        assertNotNull(user);
        assertEquals(USERNAME, user.getUsername());
        assertEquals(PASSWORD, user.getPassword());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "users"));
    }

    @Test
    public void testUpdate() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        String newName = "NewName";
        String newSurname = "NewSurname";
        String newCellphone = "123456";
        User updated = userDao.update(user.getId(), USERNAME, newName, newSurname, EMAIL, PASSWORD, newCellphone, Role.ADMIN);
        assertNotNull(updated);
        assertEquals(newName, updated.getName());
        assertEquals(newSurname, updated.getSurname());
        assertEquals(newCellphone, updated.getCellphone());
        assertEquals(Role.ADMIN, updated.getRole());
    }

    @Test
    public void testDelete() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        boolean deleted = userDao.delete(user.getId());
        assertTrue(deleted);
        assertEquals(0, JdbcTestUtils.countRowsInTable(jdbcTemplate, "users"));
    }

    @Test
    public void testFindById() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        Optional<User> found = userDao.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByEmail() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        Optional<User> found = userDao.findByEmail(EMAIL);
        assertTrue(found.isPresent());
        assertEquals(user.getEmail(), found.get().getEmail());
    }

    @Test
    public void testFindByUserName() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        Optional<User> found = userDao.findByUserName(USERNAME);
        assertTrue(found.isPresent());
        assertEquals(user.getUsername(), found.get().getUsername());
    }

    @Test
    public void testFindByVerificationToken() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        Optional<User> found = userDao.findByVerificationToken(user.getVerificationToken());
        assertTrue(found.isPresent());
        assertEquals(user.getVerificationToken(), found.get().getVerificationToken());
    }

    @Test
    public void testFindAll() {
        userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        userDao.create("User2", "Name2", "Pass2", "Last2", "email2", "cell2", Role.ADMIN);
        assertEquals(2, userDao.findAll().size());
    }

    @Test
    public void testFindByRole() {
        userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        userDao.create("User2", "Name2", "Pass2", "Last2", "email2", "cell2", Role.ADMIN);
        assertEquals(1, userDao.findByRole("user").size());
        assertEquals(1, userDao.findByRole("admin").size());
    }

    @Test
    public void testUpdateVerificationStatus() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        userDao.updateVerificationStatus(user.getId(), true);
        Optional<User> found = userDao.findById(user.getId());
        assertTrue(found.isPresent());
        assertTrue(found.get().isVerified());
    }

    @Test
    public void testUpdateVerificationToken() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        String newToken = UUID.randomUUID().toString();
        userDao.updateVerificationToken(user.getId(), newToken);
        Optional<User> found = userDao.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals(newToken, found.get().getVerificationToken());
    }

    @Test
    public void testUpdateAndFindResetPasswordToken() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        String resetToken = UUID.randomUUID().toString();
        userDao.updateResetPasswordToken(user.getId(), resetToken);
        Optional<User> found = userDao.findByResetPasswordToken(resetToken);
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testClearResetPasswordToken() {
        User user = userDao.create(USERNAME, NAME, PASSWORD, LASTNAME, EMAIL, CELLPHONE, Role.USER);
        String resetToken = UUID.randomUUID().toString();
        userDao.updateResetPasswordToken(user.getId(), resetToken);
        userDao.clearResetPasswordToken(user.getId());
        Optional<User> found = userDao.findById(user.getId());
        assertTrue(found.isPresent());
        assertNull(found.get().getResetPasswordToken());
    }
}