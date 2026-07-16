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
        // En vez de crear el mock "nice" de setUp() y comprobar después con
        // verifyZeroInteractions(), usamos un mock cuyo comportamiento por defecto es
        // explotar ante CUALQUIER interacción -- así, si el filtro llegara a tocar
        // userService para una request que no es de creación de token, el test falla
        // durante el "ejercitar" mismo.
        UserService strictUserService = mock(UserService.class, invocation -> {
            throw new AssertionError("Unexpected interaction with userService: " + invocation.getMethod().getName());
        });
        ApiBasicAuthenticationFilter strictFilter = new ApiBasicAuthenticationFilter(strictUserService);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/" + UUID.randomUUID());
        request.addHeader("Authorization", basic("demo@example.com", "secret"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        strictFilter.doFilter(request, response, new MockFilterChain());

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

        // El filtro solo autentica el SecurityContext cuando
        // userService.authenticateAndGetUser devuelve un user no-nulo, y eso sólo pasa
        // si se lo llamó con las credenciales correctas (el mock "nice" por defecto
        // devuelve null para cualquier otra combinación) -- así que estas dos
        // aserciones ya certifican la llamada correcta sin necesitar verify().
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
