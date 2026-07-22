package ar.edu.itba.paw.webapp.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final PawUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, PawUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String path = request.getRequestURI();
        boolean isApiClubPath = path != null
            && (path.startsWith("/api/clubs/") || path.startsWith("/webapp_war_exploded/api/clubs/"));
        boolean isClubMediaMutation = "PUT".equalsIgnoreCase(request.getMethod())
            && isApiClubPath
                && (path.endsWith("/cover") || path.endsWith("/layout"));

        if (isClubMediaMutation) {
            LOGGER.debug("JWT header present for {} {}: {}",
                    request.getMethod(),
                    path,
                    StringUtils.hasText(header));
        }

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7).trim();
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7).trim();
                }
                if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
                    token = token.substring(1, token.length() - 1).trim();
                }

                if (!StringUtils.hasText(token)) {
                    if (isClubMediaMutation) {
                        LOGGER.debug("JWT token empty after normalization for {} {}", request.getMethod(), path);
                    }
                    AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token");
                    return;
                }

                if (!jwtUtil.validateToken(token)) {
                    if (isClubMediaMutation) {
                        LOGGER.debug("JWT token validation failed for {} {}", request.getMethod(), path);
                    }
                    AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token");
                    return;
                } else {
                    String username = jwtUtil.getUsernameFromToken(token);
                    UsernamePasswordAuthenticationToken auth;

                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    } catch (Exception userDetailsEx) {
                        if (isClubMediaMutation) {
                            LOGGER.debug("JWT userDetails lookup failed for subject '{}' on {} {}. Falling back to subject principal.",
                                    username,
                                    request.getMethod(),
                                    path,
                                    userDetailsEx);
                        }
                        auth = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
                    }

                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(auth);
                    SecurityContextHolder.setContext(context);

                    if (isClubMediaMutation) {
                        LOGGER.debug("JWT auth set for {} {} as principal '{}'",
                                request.getMethod(),
                                path,
                                auth.getName());
                    }
                }
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                LOGGER.debug("JWT invalido para {} {}", request.getMethod(), request.getRequestURI(), ex);
                AuthErrorResponses.write(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
