package ar.edu.itba.paw.webapp.auth;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiBasicAuthenticationFilterTest {

    private UserService userService;
    private JwtUtil jwtUtil;
    private ApiBasicAuthenticationFilter filter;

    @Before
    public void setUp() {
        SecurityContextHolder.clearContext();
        userService = mock(UserService.class);
        jwtUtil = mock(JwtUtil.class);
        filter = new ApiBasicAuthenticationFilter(userService, jwtUtil);
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void missingAuthorizationLeavesRequestUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/" + UUID.randomUUID());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void basicCredentialsAuthenticateAnyApiRequestAndEmitTokenHeaders() throws Exception {
        User user = user(UUID.randomUUID(), Role.USER);
        when(userService.authenticateAndGetUser("demo@example.com", "secret")).thenReturn(user);
        when(jwtUtil.generateToken(user.getId().toString())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", basic("demo@example.com", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        // El filtro solo autentica el SecurityContext cuando
        // userService.authenticateAndGetUser devuelve un user no-nulo, y eso sólo pasa
        // si se lo llamó con las credenciales correctas (el mock "nice" por defecto
        // devuelve null para cualquier otra combinación) -- así que estas dos
        // aserciones ya certifican la llamada correcta sin necesitar verify().
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof PawUserDetails);
        assertEquals("jwt-token", response.getHeader("Paw-Auth-Token"));
        assertEquals("Bearer", response.getHeader("Paw-Token-Type"));
        assertEquals("3600", response.getHeader("Paw-Token-Expires-In"));
        assertEquals("Authorization", response.getHeader("Vary"));
    }

    @Test
    public void invalidBasicCredentialsReturnUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", basic("demo@example.com", "wrong"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void unverifiedUserReturnsForbidden() throws Exception {
        when(userService.authenticateAndGetUser("demo@example.com", "secret"))
                .thenThrow(new IllegalStateException("User not verified"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", basic("demo@example.com", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void unexpectedAuthenticationFailureReturnsServiceUnavailable() throws Exception {
        when(userService.authenticateAndGetUser("demo@example.com", "secret"))
                .thenThrow(new RuntimeException("database unavailable"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", basic("demo@example.com", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static String basic(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static User user(UUID id, Role role) {
        User user = new User(
                id,
                "demo",
                "Demo",
                "User",
                "demo@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455",
                role,
                OffsetDateTime.now(),
                "es"
        );
        user.setVerified(true);
        return user;
    }
}
