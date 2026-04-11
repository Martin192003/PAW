package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;


@Component
public class PawUserDetailsService implements UserDetailsService {
    @Autowired
    private UserService us;

    @Override
    public UserDetails loadUserByUsername(final String usernameOrEmail) throws UsernameNotFoundException {
        User user = resolveUser(usernameOrEmail);
        if (user == null) {
            throw new UsernameNotFoundException("No user by the name " + usernameOrEmail);
        }

        if (!user.isVerified()) {
            throw new UsernameNotFoundException("User is not verified");
        }

        return new PawUserDetails(user);
    }

    private User resolveUser(String usernameOrEmailOrId) {
        User user = us.findByUserName(usernameOrEmailOrId);
        if (user != null) {
            return user;
        }

        user = us.findByEmail(usernameOrEmailOrId);
        if (user != null) {
            return user;
        }

        try {
            UUID userId = UUID.fromString(usernameOrEmailOrId);
            Optional<User> maybeUser = us.findById(userId);
            return maybeUser.orElse(null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
