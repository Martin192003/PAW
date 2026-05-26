package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;


@Component
public class PawUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PawUserDetailsService.class);

    @Autowired
    private UserService us;

    @Override
    public UserDetails loadUserByUsername(final String usernameOrEmail) throws UsernameNotFoundException {
        LOGGER.warn("[AUTH] loadUserByUsername called with: '{}'", usernameOrEmail);

        User user = resolveUser(usernameOrEmail);
        if (user == null) {
            LOGGER.warn("[AUTH] User NOT FOUND in DB for input: '{}'", usernameOrEmail);
            throw new UsernameNotFoundException("No user by the name " + usernameOrEmail);
        }

        LOGGER.warn("[AUTH] User FOUND: id={}, username='{}', email='{}', verified={}",
                user.getId(), user.getUsername(), user.getEmail(), user.isVerified());

        // No lanzar aquí para usuarios no verificados: Spring Security llama a
        // isEnabled() en PawUserDetails y lanza DisabledException si retorna false.
        // ApiAuthenticationEntryPoint mapea DisabledException → 403 para que el
        // frontend redirija al flow de verificación de email.
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
