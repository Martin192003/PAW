package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.LoginResponseDto;
import ar.edu.itba.paw.model.dto.UserCreateRequestDto;
import ar.edu.itba.paw.model.dto.UserDto;
import ar.edu.itba.paw.model.dto.UserLoginRequestDto;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import ar.edu.itba.paw.webapp.auth.WebSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;


import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRestController.class);

    @Context
    private UriInfo uriInfo;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSecurity webSecurity;

    @Autowired
    private JwtUtil jwtUtil;

    @GET
    @Path("/{userId}")
    public Response getUser(@PathParam("userId") String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId inválido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Optional<User> maybe = userService.findById(id);
        if (maybe.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId inválido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        UserDto dto = UserMapper.toDto(maybe.get());
        return Response.ok(dto).build();
    }

    @POST
    public Response createUser(UserCreateRequestDto body) {
        if (body == null ||
                isBlank(body.getUsername()) ||
                isBlank(body.getName()) ||
                isBlank(body.getSurname()) ||
                isBlank(body.getEmail()) ||
                isBlank(body.getPassword()) ||
                isBlank(body.getCellphone())
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("Missing required fields"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        try {
            User user = userService.create(
                    body.getUsername().trim(),
                    body.getName().trim(),
                    body.getPassword(),
                    body.getSurname().trim(),
                    body.getEmail().trim(),
                    body.getCellphone().trim(),
                    null,
                    body.getLocale()
            );

            if (user == null) {
                return Response.serverError()
                        .entity(message("Unable to create user"))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(user.getId().toString())
                    .build();

            UserDto dto = UserMapper.toDto(user);

            return Response.created(uri)
                    .entity(dto)
                    .build();
        } catch (DuplicateEntityException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(message("User with same username or email already exists"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error creating user", e);
            return Response.serverError()
                    .entity(message("Internal server error"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @POST
    @Path("/login")
    public Response login(UserLoginRequestDto body) {
        if (body == null || isBlank(body.getUsernameOrEmail()) || isBlank(body.getPassword())) {
            try {
                String err = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(java.util.Map.of("message", "Missing credentials"));
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(err)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                LOGGER.error("JSON error", e);
                return Response.serverError().entity("{\"message\":\"Internal server error\"}").type(MediaType.APPLICATION_JSON).build();
            }
        }

        try {
            User user = userService.authenticateAndGetUser(body.getUsernameOrEmail().trim(), body.getPassword());
            if (user == null) {
                String err = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(java.util.Map.of("message", "Invalid credentials"));
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(err)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            UserDto dto = UserMapper.toDto(user);
            String token = jwtUtil.generateToken(user.getUsername());

            LoginResponseDto response = new LoginResponseDto(token, dto);

            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .findAndRegisterModules()
                    .writeValueAsString(response);

            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (IllegalStateException e) {
            try {
                String err = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(java.util.Map.of("message", "User not verified"));
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(err)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                LOGGER.error("JSON error", ex);
                return Response.serverError().entity("{\"message\":\"Internal server error\"}").type(MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            LOGGER.error("Error during login", e);
            try {
                String err = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(java.util.Map.of("message", "Internal server error"));
                return Response.serverError().entity(err).type(MediaType.APPLICATION_JSON).build();
            } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                LOGGER.error("JSON error", ex);
                return Response.serverError().entity("{\"message\":\"Internal server error\"}").type(MediaType.APPLICATION_JSON).build();
            }
        }
    }

    @GET
    @Path("/{userId}/profile")
    public Response getUserProfile(@PathParam("userId") String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId inválido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Optional<User> maybe = userService.findById(id);
        if (maybe.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(message("Usuario no encontrado"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        UserDto dto = UserMapper.toDto(maybe.get());
        return Response.ok(dto).build();
    }

    @PUT
    @Path("/{userId}")
    public Response updateUser(@PathParam("userId") String userId, UserCreateRequestDto body) {

        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId inválido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Optional<User> maybe = userService.findById(id);
        if (maybe.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(message("Usuario no encontrado"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("Body requerido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(message("Unauthorized"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();        }

        String authName = auth.getName();
        User authUser = userService.findByUserName(authName);
        if (authUser == null) {
            authUser = userService.findByEmail(authName);
        }

        if (authUser == null || !authUser.getId().equals(id)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(message("Forbidden"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();        }

        User existing = maybe.get();

        if (!isBlank(body.getUsername())) existing.setUserName(body.getUsername());
        if (!isBlank(body.getName())) existing.setName(body.getName());
        if (!isBlank(body.getSurname())) existing.setSurname(body.getSurname());
        if (!isBlank(body.getEmail())) existing.setEmail(body.getEmail());
        if (!isBlank(body.getCellphone())) existing.setCellphone(body.getCellphone());
        if (!isBlank(body.getLocale())) existing.setLocale(body.getLocale());

        try {
            User updated = userService.update(existing);
            //webSecurity.refreshAuthentication(updated);
            return Response.ok(UserMapper.toDto(updated)).build();
        } catch (DuplicateEntityException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(message("User with same username or email already exists"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error updating user " + userId, e);
            return Response.serverError()
                    .entity(message("Internal server error"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @PUT
    @Path("/{userId}/profile")
    public Response updateUserProfile(@PathParam("userId") String userId, UserCreateRequestDto body) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId inválido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Optional<User> maybe = userService.findById(id);
        if (maybe.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(message("Usuario no encontrado"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("Body requerido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(message("Unauthorized"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();        }

        String authName = auth.getName();
        User authUser = userService.findByUserName(authName);
        if (authUser == null) {
            authUser = userService.findByEmail(authName);
        }

        if (authUser == null || !authUser.getId().equals(id)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(message("Forbidden"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        User existing = maybe.get();

        if (!isBlank(body.getUsername())) existing.setUserName(body.getUsername().trim());
        if (!isBlank(body.getName())) existing.setName(body.getName().trim());
        if (!isBlank(body.getSurname())) existing.setSurname(body.getSurname().trim());
        if (!isBlank(body.getEmail())) existing.setEmail(body.getEmail().trim());
        if (!isBlank(body.getCellphone())) existing.setCellphone(body.getCellphone().trim());
        if (!isBlank(body.getLocale())) existing.setLocale(body.getLocale().trim());
        //ignoro la contraseña

        try {
            User updated = userService.update(existing);
            //webSecurity.refreshAuthentication(updated);
            return Response.ok(UserMapper.toDto(updated)).build();
        } catch (DuplicateEntityException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(message("User with same username or email already exists"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error updating user profile " + userId, e);
            return Response.serverError()
                    .entity(message("Internal server error"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String message(String msg) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(java.util.Map.of("message", msg));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return "{\"message\":\"" + msg.replace("\"", "\\\"") + "\"}";
        }
    }

}