package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.dto.ApiMessageDto;
import ar.edu.itba.paw.model.dto.AuthChallengeRequestDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.service.PasswordResetJobService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("/auth")
@Produces({ApiMediaTypes.APPLICATION_JSON})
@Consumes({ApiMediaTypes.AUTH_CHALLENGE, ApiMediaTypes.APPLICATION_JSON})
public class AuthRestController {

    private final UserService userService;
    private final PasswordResetJobService passwordResetJobService;

    @Autowired
    public AuthRestController(UserService userService,
                              PasswordResetJobService passwordResetJobService) {
        this.userService = userService;
        this.passwordResetJobService = passwordResetJobService;
    }

    @POST
    @Path("/challenges")
    public Response createChallenge(AuthChallengeRequestDto body) {
        if (body == null || isBlank(body.getChallengeType())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("challenge_type_required"))
                    .build();
        }

        switch (body.getChallengeType().trim()) {
            case "email_verification":
                return resendVerification(body);
            case "password_reset":
                return requestPasswordReset(body);
            default:
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorDto("unsupported_challenge_type"))
                        .build();
        }
    }

    private Response resendVerification(AuthChallengeRequestDto body) {
        if (body == null || isBlank(body.getUsernameOrEmail())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("username_or_email_required"))
                    .build();
        }

        boolean sent = userService.resendVerificationToken(body.getUsernameOrEmail().trim());
        if (!sent) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("verification_email_not_sent"))
                    .build();
        }

        return Response.ok(new ApiMessageDto("verification_email_sent")).build();
    }

    private Response requestPasswordReset(AuthChallengeRequestDto body) {
        if (body == null || isBlank(body.getEmail())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("email_required"))
                    .build();
        }

        passwordResetJobService.requestPasswordReset(body.getEmail().trim());
        return Response.noContent().build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
