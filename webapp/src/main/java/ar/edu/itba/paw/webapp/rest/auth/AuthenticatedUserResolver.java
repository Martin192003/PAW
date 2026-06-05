package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.SecurityContext;
import java.util.UUID;

@Component
public class AuthenticatedUserResolver {

    private final UserService userService;

    @Autowired
    public AuthenticatedUserResolver(UserService userService) {
        this.userService = userService;
    }

    public User resolve(SecurityContext securityContext) {
        if (securityContext != null && securityContext.getUserPrincipal() != null) {
            User user = resolveFromPrincipalName(securityContext.getUserPrincipal().getName());
            if (user != null) {
                return user;
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof PawUserDetails) {
            PawUserDetails userDetails = (PawUserDetails) principal;
            return userService.findById(userDetails.getId()).orElse(userDetails.getUser());
        }

        if (principal instanceof User) {
            User user = (User) principal;
            return userService.findById(user.getId()).orElse(user);
        }

        return resolveFromPrincipalName(auth.getName());
    }

    private User resolveFromPrincipalName(String principal) {
        if (principal == null || principal.trim().isEmpty()) {
            return null;
        }

        User user = userService.findByEmail(principal);
        if (user != null) {
            return user;
        }

        user = userService.findByUserName(principal);
        if (user != null) {
            return user;
        }

        try {
            return userService.findById(UUID.fromString(principal)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
