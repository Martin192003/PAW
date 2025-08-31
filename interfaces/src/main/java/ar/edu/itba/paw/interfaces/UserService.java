package ar.edu.itba.paw.interfaces;

import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Find a user by its ID.
     *
     * @param id The ID of the user.
     * @return The user with the given ID, or null if not found.
     */
    User findById(UUID id);

    /**
     * Create a new user.
     *
     * @param username The name of the user.
     * @return The created user.
     */
    User create(String username, String password, String surname, String email, String cellphone, String role);

    /*User update(User user);
    boolean delete(UUID id);

    User findByEmail(String email);
    User findByUserName(String userName);

    List<User> findAll();
    List<User> findByRole(String role);*/
}
