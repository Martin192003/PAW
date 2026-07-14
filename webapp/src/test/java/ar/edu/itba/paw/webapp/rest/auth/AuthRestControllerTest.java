package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.dto.ApiMessageDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.ResendVerificationRequestDto;
import ar.edu.itba.paw.service.PasswordResetJobService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
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
            mock(PasswordResetJobService.class),
            mock(JwtUtil.class),
            mock(AuthenticatedUserResolver.class)
    );

    @Test
    public void resendVerificationReturnsSerializableMessageDto() {
        ResendVerificationRequestDto request = new ResendVerificationRequestDto();
        request.setUsernameOrEmail("user@example.com");
        when(userService.resendVerificationToken("user@example.com")).thenReturn(true);

        Response response = controller.resendVerification(request);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ApiMessageDto);
        assertEquals("verification_email_sent", ((ApiMessageDto) response.getEntity()).getMessage());
    }

    @Test
    public void resendVerificationReturnsBadRequestWhenEmailIsNotSent() {
        ResendVerificationRequestDto request = new ResendVerificationRequestDto();
        request.setUsernameOrEmail("user@example.com");
        when(userService.resendVerificationToken("user@example.com")).thenReturn(false);

        Response response = controller.resendVerification(request);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ErrorDto);
        assertEquals("verification_email_not_sent", ((ErrorDto) response.getEntity()).getMessage());
    }
}
