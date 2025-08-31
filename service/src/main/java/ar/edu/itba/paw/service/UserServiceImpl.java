package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.UserDao;
import ar.edu.itba.paw.model.User.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.model.User.User;
import ar.edu.itba.paw.interfaces.UserService;

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
        return userDao.findById(id);
    }

    @Override
    public User create(String username, String password, String surname, String email, String cellphone, Role role) {
        return userDao.create(username, password, surname, email, cellphone, role);
    }

}
