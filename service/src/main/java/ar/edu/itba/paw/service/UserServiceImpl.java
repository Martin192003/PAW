package ar.edu.itba.paw.service;

import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
public class UserServiceImpl implements UserService {


    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;


    @Autowired
    public UserServiceImpl(final UserDao userDao, final PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(final UUID id) {
        return userDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public User create(String username, String password, String surname, String email, String cellphone, String role) {
        return userDao.create(username, passwordEncoder.encode(password), surname, email, cellphone, Role.USER);
    }

    @Override
    @Transactional
    public User update(User user) {
        return userDao.update(
                user.getId(),
                user.getUsername(),
                user.getSurname(),
                user.getEmail(),
                user.getPassword(),
                user.getCellphone(),
                user.getRole()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean delete(UUID id) {
        return userDao.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userDao.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUserName(String userName) {
        return userDao.findByUserName(userName).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(String role) {
        return userDao.findByRole(role);
    }

}
