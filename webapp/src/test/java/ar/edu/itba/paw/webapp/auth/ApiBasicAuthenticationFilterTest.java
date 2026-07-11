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

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ApiBasicAuthenticationFilterTest {

    private UserService userService;
    private ApiBasicAuthenticationFilter filter;

    @Before
    public void setUp() {
        SecurityContextHolder.clearContext();
        userService = mock(UserService.class);
        filter = new ApiBasicAuthenticationFilter(userService);
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void basicCredentialsAreIgnoredOutsideTokenCreation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/" + UUID.randomUUID() + "/profile");
        request.addHeader("Authorization", basic("demo@example.com", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verifyZeroInteractions(userService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void basicCredentialsAuthenticateTokenCreationRequest() throws Exception {
        User user = user(UUID.randomUUID(), Role.USER);
        when(userService.authenticateAndGetUser("demo@example.com", "secret")).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/tokens");
        request.addHeader("Authorization", basic("demo@example.com", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        verify(userService).authenticateAndGetUser("demo@example.com", "secret");
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof PawUserDetails);
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
