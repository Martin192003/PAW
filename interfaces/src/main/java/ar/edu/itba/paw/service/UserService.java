package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Optional<User> findById(UUID id);


    User create(String username, String name, String password, String surname, String email, String cellphone, String role);
    User update(User user);
    boolean delete(UUID id);

    User findByEmail(String email);
    User findByUserName(String userName);

    User findByVerificationToken(String token);
    boolean verifyUser(String token);
    boolean resendVerificationToken(String usernameOrEmail);

    boolean requestPasswordReset(String email);
    boolean resetPassword(String token, String newPassword);

    User updateUserField(UUID userId, String field, String value);
    User authenticateAndGetUser(String usernameOrEmail, String password);
    boolean authenticateUsernameOrEmail(String usernameOrEmail, String password);
    User findByResetPasswordToken(String token);
    boolean changePasswordForAuthenticatedUser(UUID userId, String newPassword);
}
