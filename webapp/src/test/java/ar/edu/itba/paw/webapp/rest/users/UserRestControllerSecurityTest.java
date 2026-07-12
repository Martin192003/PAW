package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.PublicUserDto;
import ar.edu.itba.paw.model.dto.UserDto;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
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

    @Before
    public void setUp() {
        controller = new UserRestController();
        userService = mock(UserService.class);
        SecurityContextHolder.clearContext();

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void otherUserGetsPublicRepresentation() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, Role.USER);
        User caller = user(UUID.randomUUID(), Role.USER);
        when(userService.findById(targetId)).thenReturn(Optional.of(target));
        authenticate(caller);

        Response response = controller.getUser(targetId.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof PublicUserDto);
    }

    @Test
    public void userGetsOwnPrivateRepresentation() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, Role.USER);
        when(userService.findById(userId)).thenReturn(Optional.of(user));

        authenticate(user);
        Response response = controller.getUser(userId.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof UserDto);
        assertEquals(user.getEmail(), ((UserDto) response.getEntity()).getEmail());
    }

    @Test
    public void adminGetsPublicRepresentationForOtherUser() {
        UUID targetId = UUID.randomUUID();
        User target = user(targetId, Role.USER);
        User admin = user(UUID.randomUUID(), Role.ADMIN);
        when(userService.findById(targetId)).thenReturn(Optional.of(target));

        authenticate(admin);
        Response response = controller.getUser(targetId.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof PublicUserDto);
    }

    @Test
    public void anonymousCallerGetsPublicRepresentation() {
        UUID targetId = UUID.randomUUID();
        when(userService.findById(targetId)).thenReturn(Optional.of(user(targetId, Role.USER)));

        Response response = controller.getUser(targetId.toString());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof PublicUserDto);
    }

    private static void authenticate(User user) {
        PawUserDetails principal = new PawUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, user.getPassword(), principal.getAuthorities())
        );
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
