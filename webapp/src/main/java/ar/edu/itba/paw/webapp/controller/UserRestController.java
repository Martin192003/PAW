package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.webapp.auth.PawUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("/users")
@Component
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRestController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PawUserDetailsService userDetailsService;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    @POST
    public Response register(final User userEntity) {
        if (userEntity == null || userEntity.getPassword() == null || userEntity.getUsername() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "Missing required fields"))
                    .build();
        }

        try {
            final User createdUser = userService.create(
                    userEntity.getUsername(),
                    userEntity.getName(),
                    userEntity.getPassword(), // Recibe la password plana del JSON
                    userEntity.getSurname(),
                    userEntity.getEmail(),
                    userEntity.getCellphone(),
                    "USER",
                    userEntity.getLocale()
            );

            final URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(createdUser.getId()))
                    .build();

            sanitizeUser(createdUser);
            return Response.created(uri).entity(createdUser).build();

        } catch (DuplicateEntityException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Collections.singletonMap("message", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error creating user", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/login")
    public Response login(final Map<String, String> credentials) {
        // Usamos Map para leer 'username' y 'password' sin depender de la Entidad
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "Username and password are required"))
                    .build();
        }

        try {
            final User loggedUser = userService.authenticateAndGetUser(username, password);

            if (loggedUser == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Collections.singletonMap("message", "Invalid credentials"))
                        .build();
            }

            // --- Integración manual con Spring Security ---
            UserDetails userDetails = userDetailsService.loadUserByUsername(loggedUser.getUsername());
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );
            // ----------------------------------------------

            sanitizeUser(loggedUser);
            return Response.ok(loggedUser).build();

        } catch (IllegalStateException e) {
            // Usuario no verificado
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Collections.singletonMap("message", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/me")
    public Response getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username;
        if (auth.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) auth.getPrincipal()).getUsername();
        } else {
            username = auth.getPrincipal().toString();
        }

        User currentUser = userService.findByUserName(username);
        if (currentUser == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        sanitizeUser(currentUser);
        return Response.ok(currentUser).build();
    }

    @GET
    @Path("/verify")
    public Response verifyAccount(@QueryParam("token") final String token) {
        if (token == null || token.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "Token is missing"))
                    .build();
        }

        final User userToVerify = userService.findByVerificationToken(token);

        if (userToVerify == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("message", "Invalid token"))
                    .build();
        }

        if (userToVerify.isVerified()) {
            return Response.ok(Collections.singletonMap("message", "User already verified")).build();
        }

        boolean isValid = userService.verifyUser(token);

        if (isValid) {
            return Response.ok(Collections.singletonMap("message", "Account verified successfully")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("message", "Invalid or expired token"))
                    .build();
        }
    }

    @POST
    @Path("/resend-verification")
    public Response resendVerification(final Map<String, String> payload) {
        String usernameOrEmail = payload.get("usernameOrEmail");

        if (usernameOrEmail == null || usernameOrEmail.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        boolean success = userService.resendVerificationToken(usernameOrEmail);

        if (success) {
            return Response.ok(Collections.singletonMap("message", "Verification email sent")).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("message", "User not found"))
                    .build();
        }
    }

    private void sanitizeUser(User user) {
        if (user == null) return;
        user.setPasswordHash(null);
        user.setVerificationToken(null);
        user.setResetPasswordToken(null);
        user.setAdminOfClubs(Collections.emptySet());
    }
}