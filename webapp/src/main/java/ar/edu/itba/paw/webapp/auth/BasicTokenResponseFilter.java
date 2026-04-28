package ar.edu.itba.paw.webapp.auth;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BasicTokenResponseFilter extends OncePerRequestFilter {
    public static final String ACCESS_TOKEN_HEADER = "X-Access-Token";
    public static final String TOKEN_TYPE_HEADER = "X-Token-Type";
    public static final String EXPIRES_IN_HEADER = "X-Expires-In";
    public static final String USER_HREF_HEADER = "X-User-Href";

    private final JwtUtil jwtUtil;

    public BasicTokenResponseFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isBasicRequest(request)) {
            addTokenHeadersIfAuthenticated(request, response);
        }

        filterChain.doFilter(request, response);
    }

    private void addTokenHeadersIfAuthenticated(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return;
        }

        String subject = auth.getName();
        String userHref = null;
        Object principal = auth.getPrincipal();
        if (principal instanceof PawUserDetails) {
            PawUserDetails userDetails = (PawUserDetails) principal;
            subject = userDetails.getId().toString();
            userHref = request.getContextPath() + "/api/users/" + userDetails.getId();
        }

        response.setHeader(ACCESS_TOKEN_HEADER, jwtUtil.generateToken(subject));
        response.setHeader(TOKEN_TYPE_HEADER, "Bearer");
        response.setHeader(EXPIRES_IN_HEADER, Long.toString(jwtUtil.getExpirationSeconds()));
        if (StringUtils.hasText(userHref)) {
            response.setHeader(USER_HREF_HEADER, userHref);
        }
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
    }

    private boolean isBasicRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return StringUtils.hasText(header) && header.regionMatches(true, 0, "Basic ", 0, 6);
    }
}
