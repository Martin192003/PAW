package ar.edu.itba.paw.webapp.rest.auth;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.AuthTokenDto;
import ar.edu.itba.paw.model.dto.AuthTokenUserDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.LoginDto;
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

@Component
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthRestController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public AuthRestController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
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

            AuthTokenDto response = new AuthTokenDto(
                    jwtUtil.generateToken(user.getId().toString()),
                    "Bearer",
                    jwtUtil.getExpirationSeconds(),
                    new AuthTokenUserDto(uriInfo.getBaseUriBuilder()
                            .path("users/{id}")
                            .resolveTemplate("id", user.getId().toString())
                            .build()
                            .toString())
            );

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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
