package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ar.edu.itba.paw.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class UserServiceImpl implements UserService {


    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final EmailService mailService;
    private final BookingDao bookingDao;
    private final ReviewsDao reviewDao;
    private final ClubDao clubDao;


    @Autowired
    public UserServiceImpl(final UserDao userDao, final PasswordEncoder passwordEncoder, final EmailService mailService, final BookingDao bookingDao, final ReviewsDao reviewDao, final ClubDao clubDao) {
        this.passwordEncoder = passwordEncoder;
        this.userDao = userDao;
        this.mailService = mailService;
        this.bookingDao = bookingDao;
        this.reviewDao = reviewDao;
        this.clubDao = clubDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(final UUID id) {
        return userDao.findById(id);
    }

    @Override
    @Transactional
    public User create(String username, String name, String password, String surname, String email, String cellphone, String role, String locale){
        try {
            User user = userDao.create(
                    username,
                    name,
                    passwordEncoder.encode(password),
                    surname,
                    email,
                    cellphone,
                    Role.USER,
                    locale
            );
            if(user == null) {
                return null;
            }
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

        if(!existingUser.getUsername().equals(user.getUsername()) && userDao.findByUserName(user.getUsername()).isPresent()){
            throw new DuplicateEntityException("user.username.duplicate");
        }
        user.setVerified(existingUser.isVerified());
        user.setVerificationToken(existingUser.getVerificationToken());
        return userDao.update(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                existingUser.getPassword(),
                user.getCellphone(),
                existingUser.getRole(),
                user.getLocale()
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
    public User findByVerificationToken(String token) {
        return userDao.findByVerificationToken(token).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByResetPasswordToken(String token) {
        return userDao.findByResetPasswordToken(token).orElse(null);
    }

    @Override
    @Transactional
    public boolean verifyUser(String token) {
        User user = userDao.findByVerificationToken(token).orElse(null);
        if (user == null) {
            return false;
        }
        userDao.updateVerificationStatus(user.getId(), true);
        return true;
    }

    @Override
    @Transactional
    public boolean resendVerificationToken(String usernameOrEmail) {
        User user = userDao.findByUserName(usernameOrEmail).orElse(null);
        if (user == null) {
            user = userDao.findByEmail(usernameOrEmail).orElse(null);
            if (user == null) {
                return false;
            }
        }

        if (user.isVerified()) {
            return false;
        }

        String newToken = UUID.randomUUID().toString();
        userDao.updateVerificationToken(user.getId(), newToken);

        try {
            mailService.sendUserRegistrationConfirmation(user.getEmail(), user.getUsername(), newToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean requestPasswordReset(String email) {
        User user = userDao.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        String resetToken = UUID.randomUUID().toString();
        userDao.updateResetPasswordToken(user.getId(), resetToken);

        try {
            mailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public Optional<User> resetPassword(String token, String newPassword) {

        Optional<User> maybeUser = userDao.findByResetPasswordToken(token);
        if (maybeUser.isEmpty()) {
            return Optional.empty();
        }

        User user = maybeUser.get();

        user.setPasswordHash(passwordEncoder.encode(newPassword));

        user.setResetPasswordToken(null);

        return Optional.of(user);
    }

    @Override
    @Transactional
    public User updateUserField(UUID userId, String field, String value) throws DuplicateEntityException {
        User existingUser = userDao.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("User not found")
        );

        switch (field.toLowerCase()) {
            case "username":
                if (!existingUser.getUsername().equals(value) && userDao.findByUserName(value).isPresent()) {
                    throw new DuplicateEntityException("user.username.duplicate");
                }
                existingUser.setUserName(value);
                break;
            case "name":
                existingUser.setName(value);
                break;
            case "surname":
                existingUser.setSurname(value);
                break;
            case "email":
                if (!existingUser.getEmail().equals(value) && userDao.findByEmail(value).isPresent()) {
                    throw new DuplicateEntityException("user.email.duplicate");
                }
                existingUser.setEmail(value);
                break;
            case "cellphone":
                existingUser.setCellphone(value);
                break;
            default:
                throw new IllegalArgumentException("Invalid field: " + field);
        }

        return userDao.update(
                existingUser.getId(),
                existingUser.getUsername(),
                existingUser.getName(),
                existingUser.getSurname(),
                existingUser.getEmail(),
                existingUser.getPassword(),
                existingUser.getCellphone(),
                existingUser.getRole(),
                existingUser.getLocale()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticateAndGetUser(String usernameOrEmail, String password) throws IllegalStateException {
        User user = userDao.findByUserName(usernameOrEmail).orElse(null);
        if (user == null) {
            user = userDao.findByEmail(usernameOrEmail).orElse(null);
            if (user == null) {
                return null;
            }
        }

        if (!user.isVerified()) {
            throw new IllegalStateException("User not verified");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        return user;
    }

    @Override
    @Transactional
    public boolean changePasswordForAuthenticatedUser(UUID userId, String newPassword) {
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        userDao.update(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                encodedPassword,
                user.getCellphone(),
                user.getRole(),
                user.getLocale()
        );
        return true;
    }

    @Override
    @Transactional
    public void updateUserLocale(UUID userId, String locale) {
        userDao.updateLocale(userId, locale);
    }

    @Override
    @Transactional
    public void sendReviewRequestsForPastBookings(User user) {
        List<Booking> bookings = bookingDao.findByUser(user.getId());
        for (Booking booking : bookings) {
            if (booking.getBookingEnd().isBefore(java.time.OffsetDateTime.now())
                    && !reviewDao.existsByUserAndClub(user.getId(), booking.getTable().getClubId())) {
                mailService.sendReviewRequestEmail(booking, user, clubDao.findById(booking.getTable().getClubId()).orElse(null));
            }
        }
    }

    @Override
    @Transactional
    public void updateRole(UUID userId, Role newRole) {
        User user = userDao.findById(userId).orElseThrow();
        userDao.update(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getSurname(),
                user.getEmail(),
                user.getPassword(),
                user.getCellphone(),
                newRole,
                user.getLocale()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByBookingId(UUID bookingId){
        Booking booking = bookingDao.findById(bookingId).orElse(null);
        if(booking == null){
            return Optional.empty();
        }
        return userDao.findById(booking.getUser().getId());
    }

}
