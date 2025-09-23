package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.persistence.UserJDBCDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
public class SecurityServiceImpl implements SecurityService{

    private final UserJDBCDao userDao;

    public SecurityServiceImpl(UserJDBCDao userDao) {
        this.userDao = userDao;
    }

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
    /*
    @Override
    public Optional<User> getLoggedUser() {
        final Optional<String> loggedUserEmail = getLoggedUserEmail();
        if(loggedUserEmail.isPresent()) {
            return userDao.findByEmail(loggedUserEmail.get());
        }
        return Optional.empty();
    }*/

    @Override
    public Optional<User> getLoggedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return Optional.of((User) principal);
            } else if (principal instanceof org.springframework.security.core.userdetails.User) {
                String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                return Optional.ofNullable(userDao.findByUserName(username).orElse(null));
            }
        }
        return Optional.empty();
    }

    @Override
    public User getVerifiedLoggedUser() {
        return getLoggedUser().orElse(null);
    }
}
