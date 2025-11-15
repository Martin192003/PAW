package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.security.core.Authentication;
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
    public Optional<User> getLoggedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return Optional.of(user);
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                String username = userDetails.getUsername();
                return userDao.findByUserName(username).or(() -> userDao.findByEmail(username));
            } else if (principal instanceof String username) {
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

        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            String username = userDetails.getUsername();
            return userDao.findByUserName(username)
                    .orElseGet(() -> userDao.findByEmail(username).orElse(null));
        }
        if (principal instanceof String username) {
            return userDao.findByUserName(username)
                    .orElseGet(() -> userDao.findByEmail(username).orElse(null));
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isClubAdmin(UUID clubId) {
        User user = getVerifiedLoggedUser();
        if (user == null) return false;
        if (!user.isVerified()) return false;
        if (user.getRole() != Role.ADMIN) return false;

        return clubService.getAdminsByClub(clubId)
                .stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAccessUser(UUID id) {
        var logged = getLoggedUser();
        if (logged.isEmpty()) return false;
        var user = logged.get();
        return user.getId().equals(id) || user.getRole() == Role.ADMIN;
    }
}
