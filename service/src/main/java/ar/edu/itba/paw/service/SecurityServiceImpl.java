package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.persistence.UserJDBCDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Service
public class SecurityServiceImpl implements SecurityService{

    private final UserJDBCDao userDao;

    public SecurityServiceImpl(UserJDBCDao userDao) {
        this.userDao = userDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getLoggedUserEmail() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        if(securityContext.getAuthentication() != null && securityContext.getAuthentication().isAuthenticated()) {
            Object principal = securityContext.getAuthentication().getPrincipal();
            if (principal instanceof User) {
                return Optional.of(((User) principal).getUsername());
            }
            else if(principal instanceof String) {
                return Optional.of((String) principal);
            }

        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getLoggedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return Optional.of((User) principal);
            } else if (principal instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                return userDao.findByUserName(username).or(() -> userDao.findByEmail(username));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public User getVerifiedLoggedUser() {
        return getLoggedUser().orElse(null);
    }
}
