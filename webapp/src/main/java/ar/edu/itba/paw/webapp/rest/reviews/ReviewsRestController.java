package ar.edu.itba.paw.webapp.rest.reviews;

import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ReviewDto;
import ar.edu.itba.paw.model.dto.ValidationGroups;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.service.ReviewService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.model.exceptions.ReviewNotFounException;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
import ar.edu.itba.paw.webapp.rest.DtoValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Path("/reviews")
@Produces({ApiMediaTypes.REVIEW, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
@Consumes({ApiMediaTypes.REVIEW, ApiMediaTypes.APPLICATION_JSON})
public class ReviewsRestController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserService userService;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    @GET
    @Produces({ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
    public Response list(@QueryParam("clubId") String clubIdStr,
                         @QueryParam("userId") String userIdStr,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size,
                         @Context SecurityContext securityContext) {
        if (clubIdStr == null && userIdStr == null) {
            return badRequest("clubId or userId is required");
        }

        UUID clubId = clubIdStr == null ? null : parseUuidOr400(clubIdStr);
        if (clubIdStr != null && clubId == null) return badRequest("club.id.invalid");
        UUID userId = userIdStr == null ? null : parseUuidOr400(userIdStr);
        if (userIdStr != null && userId == null) return badRequest("user.id.invalid");
        if (userId != null && userService.findById(userId).isEmpty()) {
            return notFound("Usuario no encontrado");
        }

        if (page < 0 || size <= 0) return badRequest("Invalid pagination parameters");

        int offset = page * size;
        UUID currentUserId = currentUserId(securityContext);
        final List<Review> reviews;
        final int total;
        if (clubId != null && userId != null) {
            Optional<Review> review = reviewService.getUserReviewForClub(userId, clubId);
            total = review.isPresent() ? 1 : 0;
            reviews = page == 0 ? review.map(List::of).orElseGet(List::of) : List.of();
        } else if (clubId != null) {
            reviews = reviewService.listByClub(clubId, null, size, offset);
            total = reviewService.countByClub(clubId);
        } else {
            total = reviewService.countByUser(userId);
            int correctedOffset = reviewService.calculateOffset(offset, size, total);
            reviews = total == 0
                    ? List.of()
                    : reviewService.listByUser(userId, null, size, correctedOffset);
        }
        List<LinkDto> body = CollectionLinks.items(reviews, review -> uriInfo.getBaseUriBuilder()
                .path("reviews/{reviewId}")
                .resolveTemplate("reviewId", review.getReviewId())
                .build());

        int totalPages = (int) Math.ceil((double) total / size);
        GenericEntity<List<LinkDto>> entity = new GenericEntity<>(body) {};
        Response.ResponseBuilder response = Response.ok(entity)
                .header("X-Total-Count", total)
                .header("Cache-Control", currentUserId == null ? "no-cache" : "private, no-store");
        if (currentUserId != null) {
            response.header("Pragma", "no-cache")
                    .header("Vary", "Authorization");
        }
        addPaginationLinks(response, page, size, totalPages);
        return response.build();
    }

    @POST
    public Response create(@QueryParam("clubId") String clubIdStr,
                           ReviewDto body,
                           @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("club.id.invalid");

        if (!DtoValidation.isValid(body, ValidationGroups.Create.class)) {
            return badRequest("Missing required fields");
        }

        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String principalName = securityContext.getUserPrincipal().getName();
        User user = userService.findByEmail(principalName);
        if (user == null) {
            user = userService.findByUserName(principalName);
        }
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            reviewService.createReview(
                    user.getId(),
                    clubId,
                    body.getRating(),
                    body.getDescription(),
                    null
            );

            Optional<Review> maybe = reviewService.getUserReviewForClub(user.getId(), clubId);
            if (maybe.isEmpty()) {
                return serverError("Unable to retrieve created review");
            }
            Review created = maybe.get();

            URI location = uriInfo.getBaseUriBuilder()
                    .path("reviews/{reviewId}")
                    .resolveTemplate("reviewId", created.getReviewId())
                    .build();

            return Response.created(location)
                    .entity(ReviewMapper.toDto(created, uriInfo, user.getId()))
                    .build();
        } catch (DuplicateEntityException e) {
            return error(Response.Status.CONFLICT, "Review already exists for this user and club");
        } catch (IllegalArgumentException e) {
            return domainArgumentError(e);
        } catch (IOException e) {
            return serverError("Internal server error");
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createMultipart(@QueryParam("clubId") String clubIdStr,
                                    @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("club.id.invalid");

        Integer rating = formInteger("rating");
        if (rating == null) {
            return badRequest("Missing required fields");
        }

        User user = authenticatedUser(securityContext);
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        try {
            reviewService.createReview(
                    user.getId(),
                    clubId,
                    rating,
                    formString("description"),
                    optionalPhoto()
            );

            Optional<Review> maybe = reviewService.getUserReviewForClub(user.getId(), clubId);
            if (maybe.isEmpty()) {
                return serverError("Unable to retrieve created review");
            }
            Review created = maybe.get();

            URI location = uriInfo.getBaseUriBuilder()
                    .path("reviews/{reviewId}")
                    .resolveTemplate("reviewId", created.getReviewId())
                    .build();

            return Response.created(location)
                    .entity(ReviewMapper.toDto(created, uriInfo, user.getId()))
                    .build();
        } catch (DuplicateEntityException e) {
            return error(Response.Status.CONFLICT, "Review already exists for this user and club");
        } catch (BadRequestException e) {
            return badRequest(messageOrDefault(e, "Solicitud invalida"));
        } catch (IllegalArgumentException e) {
            return domainArgumentError(e);
        } catch (IOException | ServletException e) {
            return serverError("Internal server error");
        }
    }

    @PUT
    @Path("/{reviewId}")
    public Response update(@PathParam("reviewId") String reviewIdStr,
                           ReviewDto body,
                           @Context SecurityContext securityContext) {
        UUID reviewId = parseUuidOr400(reviewIdStr);
        if (reviewId == null) return badRequest("review.id.invalid");

        if (!DtoValidation.isValid(body, ValidationGroups.Update.class)) return badRequest("Body requerido");

        User user = authenticatedUser(securityContext);
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        Optional<Review> existingOpt = ownedReview(reviewId, user);
        if (existingOpt.isEmpty()) {
            return notFound("Review no encontrado");
        }

        try {
            Review updated = reviewService.updateReview(
                    reviewId,
                    body.getRating(),
                    body.getDescription(),
                    null
            );
            return Response.ok(ReviewMapper.toDto(updated, uriInfo, user.getId())).build();
        } catch (ReviewNotFounException e) {
            return notFound(messageOrDefault(e, "Review no encontrado"));
        } catch (IllegalArgumentException e) {
            return domainArgumentError(e);
        } catch (IOException e) {
            return serverError("Internal server error");
        }
    }

    @PUT
    @Path("/{reviewId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateMultipart(@PathParam("reviewId") String reviewIdStr,
                                    @Context SecurityContext securityContext) {
        UUID reviewId = parseUuidOr400(reviewIdStr);
        if (reviewId == null) return badRequest("review.id.invalid");

        Integer rating = formInteger("rating");
        if (rating == null) return badRequest("Body requerido");

        User user = authenticatedUser(securityContext);
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        Optional<Review> existingOpt = ownedReview(reviewId, user);
        if (existingOpt.isEmpty()) {
            return notFound("Review no encontrado");
        }

        try {
            Review updated = reviewService.updateReview(
                    reviewId,
                    rating,
                    formString("description"),
                    optionalPhoto()
            );
            return Response.ok(ReviewMapper.toDto(updated, uriInfo, user.getId())).build();
        } catch (BadRequestException e) {
            return badRequest(messageOrDefault(e, "Solicitud invalida"));
        } catch (ReviewNotFounException e) {
            return notFound(messageOrDefault(e, "Review no encontrado"));
        } catch (IllegalArgumentException e) {
            return domainArgumentError(e);
        } catch (IOException | ServletException e) {
            return serverError("Internal server error");
        }
    }

    @DELETE
    @Path("/{reviewId}")
    public Response delete(@PathParam("reviewId") String reviewIdStr,
                           @Context SecurityContext securityContext) {
        UUID reviewId = parseUuidOr400(reviewIdStr);
        if (reviewId == null) return badRequest("review.id.invalid");

        User user = authenticatedUser(securityContext);
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        Optional<Review> existingOpt = ownedReview(reviewId, user);
        if (existingOpt.isEmpty()) return notFound("Review no encontrado");

        try {
            reviewService.deleteReview(reviewId);
            return Response.noContent().build();
        } catch (ReviewNotFounException e) {
            return notFound(messageOrDefault(e, "Review no encontrado"));
        } catch (IllegalArgumentException e) {
            return domainArgumentError(e);
        }
    }

    @GET
    @Path("/{reviewId}")
    public Response getById(@PathParam("reviewId") String reviewIdStr) {
        UUID reviewId = parseUuidOr400(reviewIdStr);
        if (reviewId == null) return badRequest("review.id.invalid");

        return reviewService.findById(reviewId)
                .map(review -> Response.ok(ReviewMapper.toDto(review, uriInfo)).build())
                .orElseGet(() -> notFound("Review no encontrado"));
    }


    private static UUID parseUuidOr400(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Response badRequest(final String msg) {
        return error(Response.Status.BAD_REQUEST, msg);
    }

    private static Response notFound(final String msg) {
        return error(Response.Status.NOT_FOUND, msg);
    }

    private static Response serverError(final String msg) {
        return error(Response.Status.INTERNAL_SERVER_ERROR, msg);
    }

    private static Response domainArgumentError(final IllegalArgumentException e) {
        String message = messageOrDefault(e, "Solicitud invalida");
        if (message.toLowerCase().contains("not found")) {
            return notFound(message);
        }
        return badRequest(message);
    }

    private static Response error(final Response.Status status, final String msg) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(message(msg))
                .build();
    }

    private static String messageOrDefault(Throwable e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }

    private static String message(String msg) {
        return "{\"message\":\"" + msg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private User authenticatedUser(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }

        String principalName = securityContext.getUserPrincipal().getName();
        User user = userService.findByEmail(principalName);
        if (user == null) {
            user = userService.findByUserName(principalName);
        }
        return user;
    }

    private UUID currentUserId(SecurityContext securityContext) {
        User user = authenticatedUser(securityContext);
        return user != null ? user.getId() : null;
    }

    private Optional<Review> ownedReview(UUID reviewId, User user) {
        return reviewService.findById(reviewId)
                .filter(review -> review.getUser() != null && user.getId().equals(review.getUser().getId()));
    }

    private String formString(String field) {
        String value = request.getParameter(field);
        if (value == null || value.trim().isEmpty()) {
            value = multipartText(field);
        }
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String multipartText(String field) {
        try {
            Part part = request.getPart(field);
            if (part == null || part.getSize() == 0) {
                return null;
            }
            return new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException | ServletException e) {
            throw new BadRequestException("Invalid multipart field " + field, e);
        }
    }

    private Integer formInteger(String field) {
        String value = formString(field);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid " + field);
        }
    }

    private Photo optionalPhoto() throws IOException, ServletException {
        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            return null;
        }

        String contentType = filePart.getContentType();
        validateImageContentType(contentType);

        byte[] data = filePart.getInputStream().readAllBytes();
        Photo photo = new Photo();
        photo.setFilename(extractFilename(filePart));
        photo.setContentType(contentType);
        photo.setData(data);
        photo.setSizeBytes((long) data.length);
        return photo;
    }

    private static void validateImageContentType(String contentType) {
        if (contentType == null) {
            throw new BadRequestException("Invalid image type");
        }

        String normalized = contentType.toLowerCase();
        if (!normalized.equals("image/jpeg")
                && !normalized.equals("image/png")
                && !normalized.equals("image/webp")) {
            throw new BadRequestException("Invalid image type");
        }
    }

    private static String extractFilename(Part part) {
        String filename = part.getSubmittedFileName();
        return filename == null || filename.trim().isEmpty() ? "review-photo" : filename.trim();
    }

    private void addPaginationLinks(Response.ResponseBuilder responseBuilder,
                                    int page,
                                    int size,
                                    int totalPages) {
        List<String> links = new ArrayList<>();
        if (page > 0) {
            links.add(pageLink(0, size, "first"));
            links.add(pageLink(page - 1, size, "prev"));
        }
        if (page + 1 < totalPages) {
            links.add(pageLink(page + 1, size, "next"));
            links.add(pageLink(totalPages - 1, size, "last"));
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
}
