package ar.edu.itba.paw.webapp.rest.reviews;

import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ReviewDto;
import ar.edu.itba.paw.service.ReviewService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
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
import java.util.stream.Collectors;

@Component
@Path("/clubs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
    @Path("/{clubId}/reviews")
    public Response listByClub(@PathParam("clubId") String clubIdStr,
                               @QueryParam("page") @DefaultValue("0") int page,
                               @QueryParam("size") @DefaultValue("20") int size) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId inválido");

        if (page < 0 || size <= 0) return badRequest("Invalid pagination parameters");

        int offset = page * size;
        List<Review> reviews = reviewService.listByClub(clubId, null, size, offset);
        List<ReviewDto> body = reviews.stream()
                .map(review -> ReviewMapper.toDto(review, uriInfo))
                .collect(Collectors.toList());

        int total = reviewService.countByClub(clubId);
        int totalPages = (int) Math.ceil((double) total / size);
        GenericEntity<List<ReviewDto>> entity = new GenericEntity<>(body) {};
        Response.ResponseBuilder response = Response.ok(entity)
                .header("X-Total-Count", total);
        addPaginationLinks(response, page, size, totalPages);
        return response.build();
    }

    @POST
    @Path("/{clubId}/reviews")
    public Response create(@PathParam("clubId") String clubIdStr,
                           ReviewDto body,
                           @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId inválido");

        if (body == null || body.getRating() == null) {
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

            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(created.getReviewId().toString())
                    .build();

            return Response.created(location)
                    .entity(ReviewMapper.toDto(created, uriInfo))
                    .build();
        } catch (DuplicateEntityException e) {
            return error(Response.Status.CONFLICT, "Review already exists for this user and club");
        } catch (Exception e) {
            return serverError("Internal server error");
        }
    }

    @POST
    @Path("/{clubId}/reviews")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createMultipart(@PathParam("clubId") String clubIdStr,
                                    @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId invalido");

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
                    .path("clubs/{clubId}/reviews/{reviewId}")
                    .resolveTemplate("clubId", clubId)
                    .resolveTemplate("reviewId", created.getReviewId())
                    .build();

            return Response.created(location)
                    .entity(ReviewMapper.toDto(created, uriInfo))
                    .build();
        } catch (DuplicateEntityException e) {
            return error(Response.Status.CONFLICT, "Review already exists for this user and club");
        } catch (Exception e) {
            return serverError("Internal server error");
        }
    }

    @PUT
    @Path("/{clubId}/reviews")
    public Response update(@PathParam("clubId") String clubIdStr,
                           ReviewDto body,
                           @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId inválido");

        if (body == null || body.getRating() == null) return badRequest("Body requerido");

        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String principalName = securityContext.getUserPrincipal().getName();
        User user = userService.findByEmail(principalName);
        if (user == null) {
            user = userService.findByUserName(principalName);
        }
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        Optional<Review> existingOpt = reviewService.getUserReviewForClub(user.getId(), clubId);

        if (existingOpt.isEmpty()) {
            return notFound("Review no encontrado");
        }

        try {
            Review updated = reviewService.updateReview(
                    existingOpt.get().getReviewId(),
                    body.getRating(),
                    body.getDescription(),
                    null
            );
            return Response.ok(ReviewMapper.toDto(updated, uriInfo)).build();
        } catch (Exception e) {
            return serverError("Internal server error");
        }
    }

    @PUT
    @Path("/{clubId}/reviews")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateMultipart(@PathParam("clubId") String clubIdStr,
                                    @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId invalido");

        Integer rating = formInteger("rating");
        if (rating == null) return badRequest("Body requerido");

        User user = authenticatedUser(securityContext);
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        Optional<Review> existingOpt = reviewService.getUserReviewForClub(user.getId(), clubId);

        if (existingOpt.isEmpty()) {
            return notFound("Review no encontrado");
        }

        try {
            Review updated = reviewService.updateReview(
                    existingOpt.get().getReviewId(),
                    rating,
                    formString("description"),
                    optionalPhoto()
            );
            return Response.ok(ReviewMapper.toDto(updated, uriInfo)).build();
        } catch (Exception e) {
            return serverError("Internal server error");
        }
    }

    @DELETE
    @Path("/{clubId}/reviews")
    public Response delete(@PathParam("clubId") String clubIdStr,
                           @Context SecurityContext securityContext) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId inválido");

        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String principalName = securityContext.getUserPrincipal().getName();
        User user = userService.findByEmail(principalName);
        if (user == null) {
            user = userService.findByUserName(principalName);
        }
        if (user == null) return Response.status(Response.Status.UNAUTHORIZED).build();

        Optional<Review> existingOpt = reviewService.getUserReviewForClub(user.getId(), clubId);
        if (existingOpt.isEmpty()) return notFound("Review no encontrado");

        try {
            reviewService.deleteReview(existingOpt.get().getReviewId());
            return Response.noContent().build();
        } catch (Exception e) {
            return serverError("Internal server error");
        }
    }

    @GET
    @Path("/{clubId}/reviews/{reviewId}")
    public Response getById(@PathParam("clubId") String clubIdStr,
                            @PathParam("reviewId") String reviewIdStr) {
        UUID clubId = parseUuidOr400(clubIdStr);
        if (clubId == null) return badRequest("clubId invÃ¡lido");

        UUID reviewId = parseUuidOr400(reviewIdStr);
        if (reviewId == null) return badRequest("reviewId invÃ¡lido");

        return reviewService.findById(reviewId)
                .filter(review -> review.getClub() != null && clubId.equals(review.getClub().getClubId()))
                .map(review -> Response.ok(ReviewMapper.toDto(review, uriInfo)).build())
                .orElseGet(() -> notFound("Review no encontrado"));
    }


    private static UUID parseUuidOr400(final String raw) {
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

    private static Response error(final Response.Status status, final String msg) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(message(msg))
                .build();
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
