package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.*;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import ar.edu.itba.paw.service.ReviewService;
import ar.edu.itba.paw.webapp.rest.reviews.ReviewMapper;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import java.util.List;
import java.util.stream.Collectors;


import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
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
    private ReviewService reviewService;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

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
                    .entity(message("userId invalido"))
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User caller = (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                ? resolveAuthenticatedUser(auth) : null;

        if (caller != null && caller.getId().equals(id)) {
            return Response.ok(UserMapper.toDto(maybe.get(), uriInfo))
                    .header("Cache-Control", "private, no-store")
                    .header("Pragma", "no-cache")
                    .header("Vary", "Authorization")
                    .build();
        }

        return Response.ok(UserMapper.toPublicDto(maybe.get(), uriInfo)).build();
    }

    @POST
    public Response createUser(UserDto body) {
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

            UserDto dto = UserMapper.toDto(user, uriInfo);

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

    @PUT
    @Path("/{userId}")
    public Response updateUser(@PathParam("userId") String userId,
                               UserDto body,
                               @Context SecurityContext securityContext) {

        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId invalido"))
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

        User authUser = authenticatedUserResolver.resolve(securityContext);
        if (authUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(message("Unauthorized"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (!authUser.getId().equals(id)) {
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

        try {
            User updated = userService.update(existing);
            UserUpdateResponseDto response = new UserUpdateResponseDto(
                    UserMapper.toDto(updated, uriInfo),
                    jwtUtil.generateToken(updated.getId().toString()),
                    "Bearer",
                    jwtUtil.getExpirationSeconds()
            );
            return Response.ok(response)
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .build();
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

    @GET
    @Path("/{userId}/reviews")
    public Response getUserReviews(@PathParam("userId") String userIdStr,
                                   @QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("size") @DefaultValue("20") int size) {
        UUID id;
        try {
            id = UUID.fromString(userIdStr);
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

        if (page < 0 || size <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("Invalid pagination parameters"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        int offset = page * size;
        List<ar.edu.itba.paw.model.Review> reviews = reviewService.obtainPaginatedReviews(id, null, size, offset);
        List<ReviewDto> dtos = reviews.stream()
                .map(review -> ReviewMapper.toDto(review, uriInfo))
                .collect(Collectors.toList());

        int total = reviewService.countByUser(id);
        GenericEntity<List<ReviewDto>> entity = new GenericEntity<>(dtos) {};
        Response.ResponseBuilder response = Response.ok(entity)
                .header("X-Total-Count", total);
        addPaginationLinks(response, page, size, total);
        return response.build();
    }
    /**
     * Resuelve el User de dominio a partir del Authentication de Spring Security.
     * Soporta PawUserDetails (tiene el UUID) y fallback por username/email.
     */
    private User resolveAuthenticatedUser(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof PawUserDetails) {
            UUID id = ((PawUserDetails) principal).getId();
            return userService.findById(id).orElse(null);
        }
        String name = auth.getName();
        User u = userService.findByUserName(name);
        if (u == null) u = userService.findByEmail(name);
        return u;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void addPaginationLinks(Response.ResponseBuilder responseBuilder, int page, int size, int total) {
        int totalPages = (int) Math.ceil((double) total / size);
        int lastPage = Math.max(0, totalPages - 1);
        List<String> links = new ArrayList<>();
        if (page > 0) {
            links.add(pageLink(0, size, "first"));
            links.add(pageLink(page - 1, size, "prev"));
        }
        if (page < lastPage) {
            links.add(pageLink(page + 1, size, "next"));
            links.add(pageLink(lastPage, size, "last"));
        }
        if (!links.isEmpty()) {
            responseBuilder.header("Link", String.join(", ", links));
        }
    }

    private String pageLink(int page, int size, String rel) {
        return "<" + uriInfo.getRequestUriBuilder()
                .replaceQueryParam("page", page)
                .replaceQueryParam("size", size)
                .build() + ">; rel=\"" + rel + "\"";
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
