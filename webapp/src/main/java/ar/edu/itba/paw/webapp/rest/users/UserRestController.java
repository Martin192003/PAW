package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.*;
import ar.edu.itba.paw.service.BookingService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
import ar.edu.itba.paw.webapp.auth.JwtUtil;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import ar.edu.itba.paw.service.ReviewService;
import ar.edu.itba.paw.webapp.rest.bookings.BookingDtoAssembler;
import ar.edu.itba.paw.webapp.rest.reviews.ReviewMapper;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import java.util.List;


import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
@Path("/users")
@Produces({ApiMediaTypes.USER, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
@Consumes({ApiMediaTypes.USER, ApiMediaTypes.APPLICATION_JSON})
public class UserRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRestController.class);
    private static final int DEFAULT_BOOKINGS_PAGE = 1;
    private static final int DEFAULT_BOOKINGS_SIZE = 10;

    @Context
    private UriInfo uriInfo;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingDtoAssembler bookingDtoAssembler;

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
                    .entity(message(duplicateUserMessage(e)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (DataIntegrityViolationException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(message("user.duplicate"))
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

    @PATCH
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
        User requested = new User(
                existing.getId(),
                existing.getUsername(),
                existing.getName(),
                existing.getSurname(),
                existing.getEmail(),
                existing.getPassword(),
                existing.getCellphone(),
                existing.getRole(),
                existing.getCreatedAt(),
                existing.getLocale()
        );

        if (!isBlank(body.getUsername())) requested.setUserName(body.getUsername().trim());
        if (!isBlank(body.getName())) requested.setName(body.getName().trim());
        if (!isBlank(body.getSurname())) requested.setSurname(body.getSurname().trim());
        if (!isBlank(body.getEmail())) requested.setEmail(body.getEmail().trim());
        if (!isBlank(body.getCellphone())) requested.setCellphone(body.getCellphone().trim());
        if (!isBlank(body.getLocale())) requested.setLocale(body.getLocale().trim());

        try {
            User updated = userService.update(requested);
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
                    .entity(message(duplicateUserMessage(e)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (DataIntegrityViolationException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(message("user.duplicate"))
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
    @Path("/{userId}/credentials/password")
    public Response changePassword(@PathParam("userId") String userId,
                                   PasswordResetDto body,
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

        if (body == null || isBlank(body.getNewPassword()) || isBlank(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("password_required"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (!body.getNewPassword().equals(body.getConfirmationPassword())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("password_confirmation_mismatch"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        boolean changed = userService.changePasswordForAuthenticatedUser(id, body.getNewPassword());
        if (!changed) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("password_unchanged"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return Response.noContent()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    @GET
    @Path("/{userId}/reviews")
    public Response getUserReviews(@PathParam("userId") String userIdStr,
                                   @QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("size") @DefaultValue("20") int size,
                                   @Context SecurityContext securityContext) {
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
        List<LinkDto> links = CollectionLinks.items(reviews, review -> uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/reviews/{reviewId}")
                .resolveTemplate("clubId", review.getClub().getClubId())
                .resolveTemplate("reviewId", review.getReviewId())
                .build());

        int total = reviewService.countByUser(id);
        GenericEntity<List<LinkDto>> entity = new GenericEntity<>(links) {};
        Response.ResponseBuilder response = Response.ok(entity)
                .header("X-Total-Count", total);
        addPaginationLinks(response, page, size, total);
        return response.build();
    }

    @GET
    @Path("/{userId}/bookings")
    public Response getUserBookings(@PathParam("userId") String userIdStr,
                                    @QueryParam("clubId") UUID clubId,
                                    @QueryParam("bookingId") String bookingId,
                                    @QueryParam("status") Status status,
                                    @QueryParam("sort") @DefaultValue("created_desc") String sort,
                                    @QueryParam("bookingDate") String bookingDate,
                                    @QueryParam("hasReceipt") Boolean hasReceipt,
                                    @QueryParam("page") @DefaultValue("1") int page,
                                    @QueryParam("size") @DefaultValue("10") int size,
                                    @Context SecurityContext securityContext) {
        final UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(message("userId invalido"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        final User authUser = authenticatedUserResolver.resolve(securityContext);
        if (authUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(message("Unauthorized"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (!authUser.getId().equals(userId)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(message("Forbidden"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        page = Math.max(page, DEFAULT_BOOKINGS_PAGE);
        size = size < 1 ? DEFAULT_BOOKINGS_SIZE : size;
        final int offset = (page - 1) * size;
        final String loginIdentifier = securityContext.getUserPrincipal().getName();

        final List<Booking> bookings = bookingService.searchForUserPaginated(
                loginIdentifier, clubId, bookingId, status, sort, bookingDate, hasReceipt, size, offset);
        final int total = bookingService.countSearchForUser(
                loginIdentifier, clubId, bookingId, status, bookingDate, hasReceipt);
        final List<LinkDto> links = CollectionLinks.items(bookings, booking -> uriInfo.getBaseUriBuilder()
                .path("bookings/{bookingId}")
                .resolveTemplate("bookingId", booking.getBookingId())
                .build());

        final int totalPages = (int) Math.ceil((double) total / size);
        final Response.ResponseBuilder response = Response.ok(new GenericEntity<List<LinkDto>>(links) {})
                .header("Cache-Control", "private, no-store")
                .header("Pragma", "no-cache")
                .header("X-Total-Count", total)
                .header("Vary", "Authorization");
        addOneBasedPaginationLinks(response, page, size, totalPages);
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

    private static String duplicateUserMessage(DuplicateEntityException e) {
        String message = e.getMessage();
        if ("user.email.duplicate".equals(message) || "user.username.duplicate".equals(message)) {
            return message;
        }
        return "user.duplicate";
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

    private void addOneBasedPaginationLinks(Response.ResponseBuilder responseBuilder, int page, int size, int totalPages) {
        List<String> links = new ArrayList<>();
        if (page > DEFAULT_BOOKINGS_PAGE) {
            links.add(pageLink(DEFAULT_BOOKINGS_PAGE, size, "first"));
            links.add(pageLink(page - 1, size, "prev"));
        }
        if (page < totalPages) {
            links.add(pageLink(page + 1, size, "next"));
            links.add(pageLink(totalPages, size, "last"));
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
    private static ErrorDto message(String msg) {
        return new ErrorDto(msg);
    }
}
