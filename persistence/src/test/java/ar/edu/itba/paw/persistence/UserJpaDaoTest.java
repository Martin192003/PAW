package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class UserJpaDaoTest {

    @Autowired private UserJpaDao dao;
    @PersistenceContext private EntityManager em;

    private User reload(UUID id) {
        em.flush();
        em.clear();
        return em.find(User.class, id);
    }

    @Test
    public void create_persists_and_normalizes_email() {
        String username = "martin";
        String rawEmail = "MiMail@example.com";
        User u = dao.create(
                username,
                "Nombre",
                "{noop}hash",
                "Apellido",
                rawEmail,
                "123456",
                Role.USER,
                "es_AR"
        );
        assertNotNull(u.getId());
        assertEquals(username, u.getUsername());
        assertEquals(rawEmail.toLowerCase(Locale.ROOT), u.getEmail());
    }

    @Test
    public void findByEmail_is_case_insensitive() {
        User u = dao.create("usr", "N", "{noop}", "S", "Case@Test.com", "123", Role.USER, "es_AR");
        Optional<User> byEmail = dao.findByEmail("case@test.com");
        assertTrue(byEmail.isPresent());
        assertEquals(u.getId(), byEmail.get().getId());
    }

    @Test
    public void findByUserName_is_case_insensitive() {
        User u = dao.create("MiUser", "N", "{noop}", "S", "a@a.com", "123", Role.USER, "es_AR");
        Optional<User> byName = dao.findByUserName("miuser");
        assertTrue(byName.isPresent());
        assertEquals(u.getId(), byName.get().getId());
    }

    @Test
    public void update_existing_updates_all_fields() {
        User u = dao.create("u1", "N", "{noop}", "S", "u1@a.com", "111", Role.USER, "es_AR");

        User updated = dao.update(
                u.getId(),
                "u2", "Name2", "Surname2",
                "New@Mail.com",
                "{noop}hash2",
                "222",
                Role.ADMIN,
                "en_US"
        );

        assertNotNull(updated);
        User db = reload(u.getId());
        assertEquals("u2", db.getUsername());
        assertEquals("new@mail.com", db.getEmail());
        assertEquals("{noop}hash2", db.getPassword());
        assertEquals("222", db.getCellphone());
        assertEquals(Role.ADMIN, db.getRole());
        assertEquals("en_US", db.getLocale());
    }

    @Test
    public void update_non_existing_returns_null() {
        User updated = dao.update(
                UUID.randomUUID(), "x", "n", "s", "x@x.com", "{noop}", "000", Role.USER, "es_AR"
        );
        assertNull(updated);
    }

    @Test
    public void delete_existing_returns_true_and_removes() {
        User u = dao.create("d1", "N", "{noop}", "S", "d1@a.com", "1", Role.USER, "es_AR");
        boolean ok = dao.delete(u.getId());
        assertTrue(ok);
        assertNull(em.find(User.class, u.getId()));
    }

    @Test
    public void delete_non_existing_returns_false() {
        boolean ok = dao.delete(UUID.randomUUID());
        assertFalse(ok);
    }

    @Test
    public void verification_token_workflow() {
        User u = dao.create("v1", "N", "{noop}", "S", "v1@a.com", "1", Role.USER, "es_AR");

        dao.updateVerificationStatus(u.getId(), true);
        User db1 = reload(u.getId());
        assertTrue(db1.isVerified());

        String newToken = UUID.randomUUID().toString();
        dao.updateVerificationToken(u.getId(), newToken);
        User db2 = reload(u.getId());
        assertEquals(newToken, db2.getVerificationToken());

        Optional<User> byTok = dao.findByVerificationToken(newToken);
        assertTrue(byTok.isPresent());
    }

    @Test
    public void reset_password_token_workflow() {
        User u = dao.create("r1", "N", "{noop}", "S", "r1@a.com", "1", Role.USER, "es_AR");
        String tok = "tok-123";
        dao.updateResetPasswordToken(u.getId(), tok);
        User db1 = reload(u.getId());
        assertEquals(tok, db1.getResetPasswordToken());

        Optional<User> byTok = dao.findByResetPasswordToken(tok);
        assertTrue(byTok.isPresent());

        dao.clearResetPasswordToken(u.getId());
        User db2 = reload(u.getId());
        assertNull(db2.getResetPasswordToken());
    }

    @Test
    public void update_locale_updates_only_locale() {
        User u = dao.create("l1", "N", "{noop}", "S", "l1@a.com", "1", Role.USER, "es_AR");
        dao.updateLocale(u.getId(), "pt_BR");
        User db = reload(u.getId());
        assertEquals("pt_BR", db.getLocale());
    }

    @Test
    public void create_duplicate_email_violates_unique_constraint() {
        dao.create("x1", "N", "{noop}", "S", "dup@a.com", "1", Role.USER, "es_AR");

        assertThrows(PersistenceException.class, () -> {
            dao.create("x2", "N", "{noop}", "S", "DUP@a.com", "2", Role.USER, "es_AR");
            em.flush();
        });
    }

    @Test
    public void create_with_null_email_throws_exception() {
        assertThrows(NullPointerException.class, () -> {
            dao.create("usr", "N", "{noop}", "S", null, "123", Role.USER, "es_AR");
        });
    }

    @Test
    public void findByEmail_non_existent_returns_empty() {
        assertFalse(dao.findByEmail("nonexistent@example.com").isPresent());
    }

    @Test
    public void updateVerificationToken_non_existing_does_not_throw_and_does_not_create_user() {
        UUID fake = UUID.randomUUID();
        dao.updateVerificationToken(fake, "tok123");
        em.flush();
        Optional<User> res = dao.findByVerificationToken("tok123");
        assertFalse(res.isPresent());
    }


    @Test
    public void testCreateDuplicateEmailFails() {
        dao.create("user1", "Name", "{noop}x", "S", "dup@example.com", "123", Role.USER, "es-AR");
        em.flush();

        assertThrows(PersistenceException.class, () -> {
            dao.create("user2", "Name", "{noop}y", "S", "DUP@example.com", "456", Role.USER, "es-AR");
            em.flush();
        });
    }

}
