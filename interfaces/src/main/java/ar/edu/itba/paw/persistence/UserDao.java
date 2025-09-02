package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    User create(String userName, String passwordHash, String surname, String email, String cellphone, Role role);
    User update(UUID userId, String userName, String surname, String email, String passwordHash, String cellphone, Role role);
    boolean delete(UUID id);

    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName);

    List<User> findAll();
    List<User> findByRole(String role);
}
