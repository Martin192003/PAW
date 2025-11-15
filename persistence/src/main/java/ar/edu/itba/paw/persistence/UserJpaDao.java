package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.*;

@Repository
public class UserJpaDao implements UserDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public User create(String username, String name, String passwordHash, String surname,
                       String email, String cellphone, Role role, String locale) {

        final User u = new User();
        u.setUserId(UUID.randomUUID());
        u.setUserName(username);
        u.setName(name);
        u.setSurname(surname);
        u.setEmail(email.trim().toLowerCase(Locale.ROOT));
        u.setPasswordHash(passwordHash);
        u.setCellphone(cellphone);
        u.setRole(role);
        u.setVerified(false);
        u.setVerificationToken(UUID.randomUUID().toString());
        u.setCreatedAt(java.time.OffsetDateTime.now());
        u.setLocale(locale);

        em.persist(u);
        return u;
    }

    @Override
    public User update(UUID userId, String userName, String name, String surname, String email,
                       String passwordHash, String cellphone, Role role, String locale) {

        User u = em.find(User.class, userId);
        if (u == null)
            return null;

        u.setUserName(userName);
        u.setName(name);
        u.setSurname(surname);
        u.setEmail(email.trim().toLowerCase(Locale.ROOT));
        u.setPasswordHash(passwordHash);
        u.setCellphone(cellphone);
        u.setRole(role);
        u.setLocale(locale);

        return em.merge(u);
    }

    @Override
    public boolean delete(UUID id) {
        User u = em.find(User.class, id);
        if (u == null)
            return false;
        em.remove(u);
        return true;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return em.createQuery("FROM User u WHERE LOWER(u.email) = LOWER(:email)", User.class)
                .setParameter("email", email).getResultList().stream().findFirst();
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        return em.createQuery("FROM User u WHERE LOWER(u.username) = LOWER(:username)", User.class)
                .setParameter("username", userName).getResultList().stream().findFirst();
    }

    @Override
    public Optional<User> findByVerificationToken(String token) {
        return em.createQuery("FROM User u WHERE u.verificationToken = :token", User.class)
                .setParameter("token", token).getResultList().stream().findFirst();
    }


    @Override
    public void updateVerificationStatus(UUID userId, boolean isVerified) {
        User u = em.find(User.class, userId);
        if (u != null) {
            u.setVerified(isVerified);
        }
    }

    @Override
    public void updateVerificationToken(UUID userId, String token) {
        User u = em.find(User.class, userId);
        if (u != null) {
            u.setVerificationToken(token);
        }
    }

    @Override
    public void updateResetPasswordToken(UUID userId, String token) {
        User u = em.find(User.class, userId);
        if (u != null) {
            u.setResetPasswordToken(token);
        }
    }

    @Override
    public Optional<User> findByResetPasswordToken(String token) {
        return em.createQuery("FROM User u WHERE u.resetPasswordToken = :token", User.class)
                .setParameter("token", token)
                .getResultList().stream().findFirst();
    }

    @Override
    public void clearResetPasswordToken(UUID userId) {
        User u = em.find(User.class, userId);
        if (u != null) {
            u.setResetPasswordToken(null);
        }
    }

    @Override
    public void updateLocale(UUID userId, String locale) {
        User u = em.find(User.class, userId);
        if (u != null) {
            u.setLocale(locale);
            em.merge(u);
        }
    }
}
