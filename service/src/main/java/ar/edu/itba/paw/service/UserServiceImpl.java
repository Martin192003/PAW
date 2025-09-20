package ar.edu.itba.paw.service;

import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;

import java.util.List;
import java.util.UUID;


@Service
public class UserServiceImpl implements UserService {


    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;


    @Autowired
    public UserServiceImpl(final UserDao userDao, final PasswordEncoder passwordEncoder, final MailService mailService) {
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        this.mailService = mailService;
    }

    @Override
    public User findById(final UUID id) {
        return userDao.findById(id).orElse(null);
    }

    @Override
    public User create(String username, String password, String surname, String email, String cellphone, String role) {
        User newUser = userDao.create(username, passwordEncoder.encode(password), surname, email, cellphone, Role.USER);
        mailService.sendUserRegistrationConfirmation(email, username);
        return newUser;
    }

    @Override
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
    public boolean delete(UUID id) {
        return userDao.delete(id);
    }

    @Override
    public User findByEmail(String email) {
        return userDao.findByEmail(email).orElse(null);
    }

    @Override
    public User findByUserName(String userName) {
        return userDao.findByUserName(userName).orElse(null);
    }

    @Override
    public List<User> findAll() {
        return userDao.findAll();
    }

    @Override
    public List<User> findByRole(String role) {
        return userDao.findByRole(role);
    }

    public boolean authenticateUsernameOrEmail(String usernameOrEmail, String password){
        User user = userDao.findByUserName(usernameOrEmail).orElse(null);
        if(user == null){
            user = userDao.findByEmail(usernameOrEmail).orElse(null);
            System.out.println("User not found: " + usernameOrEmail); // Debug log

            if(user == null) return false;
        }
        return passwordEncoder.matches(password, user.getPassword());
    }
}
