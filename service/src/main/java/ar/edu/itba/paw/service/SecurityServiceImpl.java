package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityServiceImpl implements SecurityService{

    private final UserDao userDao;
    private final ClubService clubService;

    public SecurityServiceImpl(UserDao userDao, ClubService clubService) {
        this.clubService = clubService;
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
            } else if (principal instanceof String) {
                String username = (String) principal;
                return userDao.findByUserName(username).or(() -> userDao.findByEmail(username));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public User getVerifiedLoggedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser"))
            return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof ar.edu.itba.paw.model.User) {
            return (ar.edu.itba.paw.model.User) principal;
        }

        if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            String username = springUser.getUsername();

            return userDao.findByUserName(username)
                    .orElseGet(() -> userDao.findByEmail(username)
                            .orElse(null));
        }

        if (principal instanceof String username) {
            return userDao.findByUserName(username)
                    .orElseGet(() -> userDao.findByEmail(username)
                            .orElse(null));
        }

        return null;
    }

    @Override
    public boolean isClubAdmin(UUID clubId) {
        User user = getVerifiedLoggedUser();
        if (user == null) return false;

        if (user.getRole() == Role.ADMIN) return true;

        return clubService.getAdminsByClub(clubId)
                .stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
    }
}
