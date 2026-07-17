package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ApiBasicAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiBasicAuthenticationFilter.class);

    public ApiBasicAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(method)
                || !"POST".equalsIgnoreCase(method)
                || path == null
                || !(path.endsWith("/api/auth/tokens") || path.endsWith("/api/auth/tokens/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        LOGGER.debug("ApiBasicAuthenticationFilter: request method={} path={}", request.getMethod(), request.getRequestURI());

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            LOGGER.trace("ApiBasicAuthenticationFilter: security context already has authentication, skipping");
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        LOGGER.trace("ApiBasicAuthenticationFilter: Authorization header present={}", header != null);
        if (!StringUtils.hasText(header) || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            filterChain.doFilter(request, response);
            return;
        }

        String[] credentials = decodeBasicCredentials(header.substring(6).trim());
        if (credentials == null) {
            LOGGER.warn("ApiBasicAuthenticationFilter: failed to decode Basic credentials");
            AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_credentials");
            return;
        }

        final String username = credentials[0];
        // Enmascarado porque username suele ser el email real del usuario (PII);
        // no queda en texto plano ni siquiera en logs de nivel debug.
        LOGGER.debug("ApiBasicAuthenticationFilter: attempting authentication for username={}", mask(username));

        try {
            User user = userService.authenticateAndGetUser(credentials[0], credentials[1]);
            if (user == null) {
                // DEBUG (no INFO): un intento de login fallido (password incorrecta,
                // etc.) es un evento rutinario, no algo que deba quedar en el nivel
                // que se loguea siempre en producción.
                LOGGER.debug("ApiBasicAuthenticationFilter: authentication failed for username={}", mask(username));
                AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_credentials");
                return;
            }

            PawUserDetails principal = new PawUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // DEBUG (no INFO): cada login exitoso es tráfico normal, no un evento
            // digno del nivel que se loguea siempre en producción.
            LOGGER.debug("ApiBasicAuthenticationFilter: authentication succeeded for username={}", mask(username));

            filterChain.doFilter(request, response);
        } catch (IllegalStateException ex) {
            LOGGER.warn("ApiBasicAuthenticationFilter: account disabled for username={}", mask(username), ex);
            AuthErrorResponses.write(response, HttpServletResponse.SC_FORBIDDEN, "account_disabled");
        } catch (Exception ex) {
            LOGGER.warn("ApiBasicAuthenticationFilter: unexpected authentication error for username={}: {}", mask(username), ex.getMessage());
            AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_credentials");
        }
    }

    /**
     * Enmascara un identificador potencialmente sensible (username/email) antes
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

    private String[] decodeBasicCredentials(String base64Credentials) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Credentials);
            String raw = new String(decoded, StandardCharsets.UTF_8);
            int separator = raw.indexOf(':');
            if (separator < 0) {
                LOGGER.debug("ApiBasicAuthenticationFilter: basic credentials missing ':' separator");
                return null;
            }

            String username = raw.substring(0, separator).trim();
            String password = raw.substring(separator + 1);
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                LOGGER.debug("ApiBasicAuthenticationFilter: username or password empty after decode (username present={})", StringUtils.hasText(username));
                return null;
            }

            return new String[] { username, password };
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}



