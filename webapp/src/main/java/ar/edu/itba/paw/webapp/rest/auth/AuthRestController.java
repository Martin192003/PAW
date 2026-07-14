package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ApiMessageDto;
import ar.edu.itba.paw.model.dto.AuthChallengeRequestDto;
import ar.edu.itba.paw.model.dto.AuthTokenDto;
import ar.edu.itba.paw.model.dto.AuthTokenRequestDto;
import ar.edu.itba.paw.model.dto.AuthTokenUserDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.service.PasswordResetJobService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

@Component
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthRestController {

    private final UserService userService;
    private final PasswordResetJobService passwordResetJobService;
    private final JwtUtil jwtUtil;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public AuthRestController(UserService userService,
                              PasswordResetJobService passwordResetJobService,
                              JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordResetJobService = passwordResetJobService;
        this.jwtUtil = jwtUtil;
    }

    @POST
    @Path("/tokens")
    public Response createToken(AuthTokenRequestDto body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("Missing required fields"))
                    .build();
        }

        String grantType = isBlank(body.getGrantType()) ? "password" : body.getGrantType().trim();
        switch (grantType) {
            case "password":
                return passwordToken(body);
            case "email_verification":
                return verificationToken(body);
            case "password_reset":
                return passwordResetToken(body);
            default:
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorDto("unsupported_grant_type"))
                        .build();
        }
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

    private Response passwordToken(AuthTokenRequestDto body) {
        String loginIdentifier = !isBlank(body.getUsernameOrEmail()) ? body.getUsernameOrEmail() : body.getEmail();
        if (isBlank(loginIdentifier) || isBlank(body.getPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("Missing required fields"))
                    .build();
        }

        try {
            User user = userService.authenticateAndGetUser(loginIdentifier.trim(), body.getPassword());
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorDto("invalid_credentials"))
                        .build();
            }

            AuthTokenDto response = tokenFor(user);

            return Response.ok(response)
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .build();
        } catch (IllegalStateException ex) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorDto("account_disabled"))
                    .build();
        }
    }

    private Response verificationToken(AuthTokenRequestDto body) {
        String tokenOrCode = !isBlank(body.getToken()) ? body.getToken() : body.getCode();
        if (isBlank(tokenOrCode)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("verification_token_required"))
                    .build();
        }

        return buildVerificationLoginResponse(tokenOrCode.trim());
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

    private Response passwordResetToken(AuthTokenRequestDto body) {
        if (isBlank(body.getToken())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("token_required"))
                    .build();
        }

        if (isBlank(body.getNewPassword()) || isBlank(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_required"))
                    .build();
        }

        if (!body.getNewPassword().equals(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_confirmation_mismatch"))
                    .build();
        }

        Optional<User> updated = userService.resetPassword(body.getToken().trim(), body.getNewPassword());
        if (updated.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("invalid_or_expired_token"))
                    .build();
        }

        return Response.ok(tokenFor(updated.get()))
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    private Response buildVerificationLoginResponse(String tokenOrCode) {
        User user = userService.findByVerificationToken(tokenOrCode);
        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("invalid_verification_token"))
                    .build();
        }

        if (!user.isVerified() && !userService.verifyUser(tokenOrCode)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("invalid_verification_token"))
                    .build();
        }

        return Response.ok(tokenFor(user))
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    private AuthTokenDto tokenFor(User user) {
        String href = uriInfo.getBaseUriBuilder()
                .path("users/{id}")
                .resolveTemplate("id", user.getId().toString())
                .build()
                .toString();

        return new AuthTokenDto(
                jwtUtil.generateToken(user.getId().toString()),
                "Bearer",
                jwtUtil.getExpirationSeconds(),
                new AuthTokenUserDto(href, user.getId())
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
