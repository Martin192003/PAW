package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.dto.ApiMessageDto;
import ar.edu.itba.paw.model.dto.AuthChallengeRequestDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.service.PasswordResetJobService;
import ar.edu.itba.paw.service.UserService;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthRestControllerTest {

    private final UserService userService = mock(UserService.class);
    private final AuthRestController controller = new AuthRestController(
            userService,
            mock(PasswordResetJobService.class)
    );

    @Test
    public void resendVerificationReturnsSerializableMessageDto() {
        AuthChallengeRequestDto request = new AuthChallengeRequestDto();
        request.setChallengeType("email_verification");
        request.setUsernameOrEmail("user@example.com");
        when(userService.resendVerificationToken("user@example.com")).thenReturn(true);

        Response response = controller.createChallenge(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ApiMessageDto);
        assertEquals("verification_email_sent", ((ApiMessageDto) response.getEntity()).getMessage());
    }

    @Test
    public void resendVerificationReturnsBadRequestWhenEmailIsNotSent() {
        AuthChallengeRequestDto request = new AuthChallengeRequestDto();
        request.setChallengeType("email_verification");
        request.setUsernameOrEmail("user@example.com");
        when(userService.resendVerificationToken("user@example.com")).thenReturn(false);

        Response response = controller.createChallenge(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorDto);
        assertEquals("verification_email_not_sent", ((ErrorDto) response.getEntity()).getMessage());
    }
}
