package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ApiMessageDto;
import ar.edu.itba.paw.model.dto.AuthTokenDto;
import ar.edu.itba.paw.model.dto.AuthTokenUserDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.LoginDto;
import ar.edu.itba.paw.model.dto.PasswordResetDto;
import ar.edu.itba.paw.model.dto.PasswordResetRequestDto;
import ar.edu.itba.paw.model.dto.ResendVerificationRequestDto;
import ar.edu.itba.paw.model.dto.UserVerificationRequestDto;
import ar.edu.itba.paw.service.PasswordResetJobService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public AuthRestController(UserService userService,
                              PasswordResetJobService passwordResetJobService,
                              JwtUtil jwtUtil,
                              AuthenticatedUserResolver authenticatedUserResolver) {
        this.userService = userService;
        this.passwordResetJobService = passwordResetJobService;
        this.jwtUtil = jwtUtil;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @POST
    @Path("/tokens")
    public Response createToken(LoginDto loginDto) {
        if (loginDto == null || isBlank(loginDto.getEmail()) || isBlank(loginDto.getPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("Missing required fields"))
                    .build();
        }

        try {
            User user = userService.authenticateAndGetUser(loginDto.getEmail().trim(), loginDto.getPassword());
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

    @GET
    @Path("/verifications")
    public Response verifyFromLink(@QueryParam("token") String token) {
        if (isBlank(token)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("verification_token_required"))
                    .build();
        }

        return buildVerificationLoginResponse(token.trim());
    }

    @POST
    @Path("/verifications")
    public Response verifyFromCode(UserVerificationRequestDto body) {
        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("body_required"))
                    .build();
        }

        String tokenOrCode = !isBlank(body.getToken()) ? body.getToken() : body.getCode();
        if (isBlank(tokenOrCode)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("verification_token_required"))
                    .build();
        }

        return buildVerificationLoginResponse(tokenOrCode.trim());
    }

    @POST
    @Path("/verification-requests")
    public Response resendVerification(ResendVerificationRequestDto body) {
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

    @POST
    @Path("/password-reset-requests")
    public Response requestPasswordReset(PasswordResetRequestDto body) {
        if (body == null || isBlank(body.getEmail())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("email_required"))
                    .build();
        }

        passwordResetJobService.requestPasswordReset(body.getEmail().trim());
        return Response.noContent().build();
    }

    @POST
    @Path("/password-resets")
    public Response resetPassword(@QueryParam("token") String token, PasswordResetDto body) {
        if (isBlank(token)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("token_required"))
                    .build();
        }

        if (body == null || isBlank(body.getNewPassword()) || isBlank(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_required"))
                    .build();
        }

        if (!body.getNewPassword().equals(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_confirmation_mismatch"))
                    .build();
        }

        Optional<User> updated = userService.resetPassword(token.trim(), body.getNewPassword());
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

    /**
     * Cambio de contraseña para un usuario que ya tiene sesion iniciada (ej.
     * desde /profile). A diferencia de /password-resets, no requiere token de
     * mail: la identidad se toma del JWT de la request, no de un link.
     * Reutiliza PasswordResetDto (mismos campos newPassword/confirmationPassword)
     * y UserServiceImpl.changePasswordForAuthenticatedUser, que ya existia pero
     * no tenia ningun endpoint que lo expusiera.
     */
    @POST
    @Path("/password-changes")
    public Response changePassword(PasswordResetDto body, @Context SecurityContext securityContext) {
        User authUser = authenticatedUserResolver.resolve(securityContext);
        if (authUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorDto("unauthorized"))
                    .build();
        }

        if (body == null || isBlank(body.getNewPassword()) || isBlank(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_required"))
                    .build();
        }

        if (!body.getNewPassword().equals(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_confirmation_mismatch"))
                    .build();
        }

        boolean changed = userService.changePasswordForAuthenticatedUser(authUser.getId(), body.getNewPassword());
        if (!changed) {
            // false tambien puede significar "usuario no encontrado", pero eso no
            // deberia pasar nunca aca: ya se resolvio un usuario autenticado arriba.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("password_unchanged"))
                    .build();
        }

        return Response.noContent()
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
                new AuthTokenUserDto(
                        href,
                        user.getId(),
                        user.getUsername(),
                        user.getName(),
                        user.getSurname(),
                        user.getEmail(),
                        user.getCellphone(),
                        user.getRole() != null ? user.getRole().name() : null,
                        user.isVerified(),
                        user.getCreatedAt(),
                        user.getLocale()
                )
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
