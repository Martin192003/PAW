package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class UserJpaDaoTest {

    @Autowired
    private UserJpaDao userDao;

    @PersistenceContext
    private EntityManager em;

    private User user;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        user = persistUser("user1", "Name1", "Last1", "email1@test.com", "{noop}h1");
        em.flush();
    }

    // ---------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------

    @Test
    public void testCreate() {
        User created = userDao.create(
                "user2",
                "Name2",
                "h2",
                "Last2",
                "email2@test.com",
                "456",
                Role.ADMIN,
                "en"
        );
        em.flush();

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("user2", created.getUsername());
        assertEquals("name2", created.getName().toLowerCase());
        assertEquals("email2@test.com", created.getEmail());
        assertEquals(Role.ADMIN, created.getRole());
        assertFalse(created.isVerified());
    }

    @Test
    public void testCreateWithNullUsernameThrows() {
        assertThrows(PersistenceException.class, () -> {
            userDao.create(
                    null,
                    "Name",
                    "hash",
                    "Last",
                    "email@test.com",
                    "123",
                    Role.USER,
                    "es"
            );
            em.flush();
        });
    }

    @Test
    public void testCreateWithDuplicateEmailThrows() {
        assertThrows(PersistenceException.class, () -> {
            userDao.create(
                    "another",
                    "Name",
                    "hash",
                    "Last",
                    user.getEmail(),
                    "123",
                    Role.USER,
                    "es"
            );
            em.flush();
        });
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    @Test
    public void testUpdate() {
        User u = userDao.update(
                user.getId(),
                "newUsername",
                "NewName",
                "NewLast",
                "new@email.com",
                "newHash",
                "999",
                Role.ADMIN,
                "pt"
        );

        assertNotNull(u);
        assertEquals("newusername", u.getUsername().toLowerCase());
        assertEquals("newname", u.getName().toLowerCase());
        assertEquals("newlast", u.getSurname().toLowerCase());
        assertEquals("new@email.com", u.getEmail());
        assertEquals("newHash", u.getPassword());
        assertEquals("999", u.getCellphone());
        assertEquals(Role.ADMIN, u.getRole());
        assertEquals("pt", u.getLocale());
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        User u = userDao.update(
                UUID.randomUUID(),
                "usr",
                "Name",
                "Last",
                "mail@test.com",
                "hash",
                "000",
                Role.USER,
                "es"
        );

        assertNull(u);
    }

    @Test
    public void testUpdateWithDuplicateEmailThrows() {
        User other = persistUser("u2", "N", "L", "dup@test.com", "{noop}");

        assertThrows(PersistenceException.class, () -> {
            userDao.update(
                    user.getId(),
                    "usr",
                    "Name",
                    "Last",
                    "dup@test.com",
                    "hash",
                    "000",
                    Role.USER,
                    "es"
            );
            em.flush();
        });
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    @Test
    public void testDelete() {
        boolean deleted = userDao.delete(user.getId());
        em.flush();

        assertTrue(deleted);
        assertEquals(0L, countUsers());
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        boolean deleted = userDao.delete(UUID.randomUUID());
        assertFalse(deleted);
        assertEquals(1L, countUsers());
    }

    // ---------------------------------------------------------
    // FIND BY ID
    // ---------------------------------------------------------

    @Test
    public void testFindById() {
        Optional<User> found = userDao.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<User> found = userDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ---------------------------------------------------------
    // FIND BY EMAIL
    // ---------------------------------------------------------

    @Test
    public void testFindByEmail() {
        Optional<User> found = userDao.findByEmail(user.getEmail());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByEmailNotFound() {
        assertFalse(userDao.findByEmail("notexist@test.com").isPresent());
    }

    // ---------------------------------------------------------
    // FIND BY USERNAME
    // ---------------------------------------------------------

    @Test
    public void testFindByUsername() {
        Optional<User> found = userDao.findByUserName(user.getUsername());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByUsernameNotFound() {
        assertFalse(userDao.findByUserName("unknown").isPresent());
    }

    // ---------------------------------------------------------
    // VERIFICATION TOKEN & STATUS
    // ---------------------------------------------------------

    @Test
    public void testUpdateVerificationStatus() {
        userDao.updateVerificationStatus(user.getId(), true);
        em.flush();

        User u = em.find(User.class, user.getId());
        assertTrue(u.isVerified());
    }

    @Test
    public void testUpdateVerificationStatusNotFoundDoesNothing() {
        userDao.updateVerificationStatus(UUID.randomUUID(), true);
        em.flush();

        User u = em.find(User.class, user.getId());
        assertFalse(u.isVerified());
    }

    @Test
    public void testUpdateVerificationToken() {
        userDao.updateVerificationToken(user.getId(), "tok123");
        em.flush();

        User u = em.find(User.class, user.getId());
        assertEquals("tok123", u.getVerificationToken());
    }

    @Test
    public void testFindByVerificationToken() {
        user.setVerificationToken("hello");
        em.flush();

        Optional<User> found = userDao.findByVerificationToken("hello");
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
    }

    @Test
    public void testFindByVerificationTokenNotFound() {
        assertFalse(userDao.findByVerificationToken("xxxxx").isPresent());
    }

    // ---------------------------------------------------------
    // RESET PASSWORD TOKEN
    // ---------------------------------------------------------

    @Test
    public void testUpdateResetPasswordToken() {
        userDao.updateResetPasswordToken(user.getId(), "reset123");
        em.flush();

        User u = em.find(User.class, user.getId());
        assertEquals("reset123", u.getResetPasswordToken());
    }

    @Test
    public void testFindByResetPasswordToken() {
        user.setResetPasswordToken("zzz");
        em.flush();

        Optional<User> found = userDao.findByResetPasswordToken("zzz");
        assertTrue(found.isPresent());
    }

    @Test
    public void testFindByResetPasswordTokenNotFound() {
        assertFalse(userDao.findByResetPasswordToken("aaaa").isPresent());
    }

    @Test
    public void testClearResetPasswordToken() {
        user.setResetPasswordToken("resetMe");
        em.flush();

        userDao.clearResetPasswordToken(user.getId());
        em.flush();

        User u = em.find(User.class, user.getId());
        assertNull(u.getResetPasswordToken());
    }

    // ---------------------------------------------------------
    // UPDATE LOCALE
    // ---------------------------------------------------------

    @Test
    public void testUpdateLocale() {
        userDao.updateLocale(user.getId(), "fr");
        em.flush();

        User u = em.find(User.class, user.getId());
        assertEquals("fr", u.getLocale());
    }

    @Test
    public void testUpdateLocaleNotFoundDoesNothing() {
        userDao.updateLocale(UUID.randomUUID(), "it");
        em.flush();

        User u = em.find(User.class, user.getId());
        assertEquals("es", u.getLocale());
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private User persistUser(String username, String name, String surname, String email,
                             String passwordHash) {
        User u = new User();
        u.setUserId(UUID.randomUUID());
        u.setUserName(username);
        u.setName(name);
        u.setSurname(surname);
        u.setEmail(email);
        u.setPasswordHash(passwordHash);
        u.setCellphone("123");
        u.setRole(Role.USER);
        u.setVerified(false);
        u.setVerificationToken(UUID.randomUUID().toString());
        u.setCreatedAt(OffsetDateTime.now());
        u.setLocale("es");
        em.persist(u);
        return u;
    }

    private long countUsers() {
        return em.createQuery("select count(u) from User u", Long.class).getSingleResult();
    }
}
