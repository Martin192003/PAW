package ar.edu.itba.paw.service;

import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;

import java.util.Optional;
import java.util.UUID;


@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Autowired
    public UserServiceImpl(final UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User findById(final UUID id) {
        return userDao.findById(id).orElse(null);
    }

    @Override
    public User create(String username, String password, String surname, String email, String cellphone, String role) {
        return userDao.create(username, password, surname, email, cellphone, Role.valueOf(role.toUpperCase()));
    }

}
