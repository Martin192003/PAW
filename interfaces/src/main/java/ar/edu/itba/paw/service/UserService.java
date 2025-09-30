package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User findById(UUID id);


    User create(String username, String name, String password, String surname, String email, String cellphone, String role);
    User update(User user);
    boolean delete(UUID id);

    User findByEmail(String email);
    User findByUserName(String userName);

    List<User> findAll();
    List<User> findByRole(String role);
    User findByVerificationToken(String token);
    void verifyUser(String token);
    boolean authenticateUsernameOrEmail(String usernameOrEmail, String password);
}
