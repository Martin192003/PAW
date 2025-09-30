package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final EmailServiceImpl mailService;


    @Autowired
    public UserServiceImpl(final UserDao userDao, final PasswordEncoder passwordEncoder, final EmailServiceImpl mailService) {
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        this.mailService = mailService;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(final UUID id) {
        return userDao.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public User create(String username, String name, String password, String surname, String email, String cellphone, String role){
        try {
            User user = userDao.create(
                    username,
                    name,
                    passwordEncoder.encode(password),
                    surname,
                    email,
                    cellphone,
                    Role.USER
            );
            mailService.sendUserRegistrationConfirmation(email, username, user.getVerificationToken());
            return user;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEntityException("user.email.duplicate", ex);
        }
    }

    @Override
    @Transactional
    public User update(User user) {
        User existingUser = userDao.findById(user.getId()).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );
        user.setVerified(existingUser.isVerified());
        user.setVerificationToken(existingUser.getVerificationToken());
        return userDao.update(
                user.getId(),
                user.getUsername(),
                user.getName(),
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

    @Override
    @Transactional(readOnly = true)
    public User findByVerificationToken(String token) {
        return userDao.findByVerificationToken(token).orElse(null);
    }

    @Override
    @Transactional
    public void verifyUser(String token) {
        User user = userDao.findByVerificationToken(token).orElseThrow(() ->
                new IllegalArgumentException("Invalid verification token")
        );
        userDao.updateVerificationStatus(user.getId(), true);
    }

    public boolean authenticateUsernameOrEmail(String usernameOrEmail, String password){
        User user = userDao.findByUserName(usernameOrEmail).orElse(null);
        if(user == null){
            user = userDao.findByEmail(usernameOrEmail).orElse(null);
            if(user == null) return false;
        }
        if(!user.isVerified()){
            throw new IllegalStateException("User not verified");
        }
        return passwordEncoder.matches(password, user.getPassword());
    }
}
