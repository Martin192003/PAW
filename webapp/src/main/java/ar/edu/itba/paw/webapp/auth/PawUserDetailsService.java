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
        // DEBUG (no WARN): esto se ejecuta en cada intento de login, no es una
        // condicion excepcional. Y el identificador se enmascara porque puede
        // ser un email/username real (PII) que no debe quedar en texto plano
        // ni siquiera en logs de nivel debug.
        LOGGER.debug("[AUTH] loadUserByUsername called with: '{}'", mask(usernameOrEmail));

        User user = resolveUser(usernameOrEmail);
        if (user == null) {
            LOGGER.debug("[AUTH] User NOT FOUND in DB for input: '{}'", mask(usernameOrEmail));
            throw new UsernameNotFoundException("No user by the name " + usernameOrEmail);
        }

        LOGGER.debug("[AUTH] User FOUND: id={}, username='{}', email='{}', verified={}",
                user.getId(), mask(user.getUsername()), mask(user.getEmail()), user.isVerified());

        // No lanzar aquí para usuarios no verificados: Spring Security llama a
        // isEnabled() en PawUserDetails y lanza DisabledException si retorna false.
        // ApiAuthenticationEntryPoint mapea DisabledException → 403 para que el
        // frontend redirija al flow de verificación de email.
        return new PawUserDetails(user);
    }

    /**
     * Enmascara un identificador potencialmente sensible (email o username) antes
     * de loguearlo: conserva sólo los primeros caracteres para poder correlacionar
     * eventos en debug sin exponer el dato completo.
     */
    private static String mask(String value) {
        if (value == null) {
            return null;
        }
        int visible = Math.min(2, value.length());
        return value.substring(0, visible) + "***";
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
