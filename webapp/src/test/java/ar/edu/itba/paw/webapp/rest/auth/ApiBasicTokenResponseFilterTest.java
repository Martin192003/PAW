package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.webapp.auth.ApiTokenHeaders;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiBasicTokenResponseFilterTest {

    @Test
    public void rememberedBasicTokenIsCopiedToJaxRsResponseHeaders() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        when(jwtUtil.generateToken(userId.toString())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        ApiTokenHeaders.remember(request, jwtUtil, user);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        when(responseContext.getHeaders()).thenReturn(headers);

        ApiBasicTokenResponseFilter filter = new ApiBasicTokenResponseFilter();
        ReflectionTestUtils.setField(filter, "request", request);

        filter.filter(mock(ContainerRequestContext.class), responseContext);

        assertEquals("jwt-token", headers.getFirst("Paw-Auth-Token"));
        assertEquals("Bearer", headers.getFirst("Paw-Token-Type"));
        assertEquals(3600L, headers.getFirst("Paw-Token-Expires-In"));
        assertEquals("Authorization", headers.getFirst("Vary"));
    }

    private static User user(UUID id) {
        User user = new User(
                id,
                "demo",
                "Demo",
                "User",
                "demo@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455",
                Role.USER,
                OffsetDateTime.now(),
                "es"
        );
        user.setVerified(true);
        return user;
    }
}
