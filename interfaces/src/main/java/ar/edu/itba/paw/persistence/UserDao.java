package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    User create(String username, String name, String passwordHash, String surname, String email, String cellphone, Role role, String locale);
    User update(UUID userId, String userName, String name, String surname, String email, String passwordHash, String cellphone, Role role, String locale);
    boolean delete(UUID id);

    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName);
    Optional<User> findByVerificationToken(String token);

    void updateVerificationStatus(UUID userId, boolean isVerified);
    void updateVerificationToken(UUID userId, String token);

    void updateResetPasswordToken(UUID userId, String token);
    Optional<User> findByResetPasswordToken(String token);
    void clearResetPasswordToken(UUID userId);
    public void updateLocale(UUID userId, String locale);
}
