package ar.edu.itba.paw.interfaces;

import ar.edu.itba.paw.model.User.User;
import ar.edu.itba.paw.model.User.Role;


public interface UserDao {

     /*Create a new user.
     @param username The name of the user.
     @return The created user.
    */
    User create(String username,String password, String surname, String email, String cellphone, Role role);

    /*Find a user by its ID.
    @param id The ID of the user.
    @return The user with the given ID, or null if not found.
     */
    User findById(long id);
}