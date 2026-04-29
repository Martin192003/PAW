package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PasswordResetJobService {

    private final UserDao userDao;
    private final EmailService mailService;

    public PasswordResetJobService(UserDao userDao, EmailService mailService) {
        this.userDao = userDao;
        this.mailService = mailService;
    }

    @Async
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userDao.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }

        String resetToken = UUID.randomUUID().toString();
        userDao.updateResetPasswordToken(user.getId(), resetToken);
        mailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), resetToken);
    }
}
