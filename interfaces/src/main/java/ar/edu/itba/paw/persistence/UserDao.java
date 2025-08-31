package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.UUID;

public interface UserDao {
    User create(String userName, String passwordHash, String surname, String email, String cellphone, String role);
    User update(User user);
    boolean delete(UUID id);

    User findById(UUID id);
    User findByEmail(String email);
    User findByUserName(String userName);

    List<User> findAll();
    List<User> findByRole(String role);
}
