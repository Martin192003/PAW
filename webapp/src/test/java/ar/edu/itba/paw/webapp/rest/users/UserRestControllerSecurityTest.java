package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.UserDto;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRestControllerSecurityTest {

    private UserRestController controller;
    private UserService userService;
    private AuthenticatedUserResolver authenticatedUserResolver;
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        controller = new UserRestController();
        userService = mock(UserService.class);
        authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
        securityContext = mock(SecurityContext.class);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "authenticatedUserResolver", authenticatedUserResolver);
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void otherUserCannotViewPrivateProfile() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, Role.USER);
        User caller = user(UUID.randomUUID(), Role.USER);
        when(userService.findById(targetId)).thenReturn(Optional.of(target));
        when(authenticatedUserResolver.resolve(securityContext)).thenReturn(caller);

        Response response = controller.getUserProfile(targetId.toString(), securityContext);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void userCanViewOwnPrivateProfile() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, Role.USER);
        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(authenticatedUserResolver.resolve(securityContext)).thenReturn(user);

        Response response = controller.getUserProfile(userId.toString(), securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof UserDto);
        assertEquals(user.getEmail(), ((UserDto) response.getEntity()).getEmail());
    }

    @Test
    public void adminCanViewOtherPrivateProfile() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, Role.USER);
        User admin = user(UUID.randomUUID(), Role.ADMIN);
        when(userService.findById(targetId)).thenReturn(Optional.of(target));
        when(authenticatedUserResolver.resolve(securityContext)).thenReturn(admin);

        Response response = controller.getUserProfile(targetId.toString(), securityContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof UserDto);
    }

    @Test
    public void anonymousCallerCannotViewPrivateProfile() {
        UUID targetId = UUID.randomUUID();
        when(userService.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));
        when(authenticatedUserResolver.resolve(securityContext)).thenReturn(null);

        Response response = controller.getUserProfile(targetId.toString(), securityContext);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private static User user(UUID id, Role role) {
        User user = new User(
                id,
                "user-" + id,
                "Name",
                "Surname",
                id + "@example.com",
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
