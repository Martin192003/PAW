package ar.edu.itba.paw.webapp.rest.reviews;

import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.ReviewDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

public final class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewDto toDto(final Review review) {
        if (review == null) return null;

        ReviewDto dto = new ReviewDto();
        dto.setReviewId(review.getReviewId());
        dto.setUserId(review.getUser() != null ? review.getUser().getId() : null);
        dto.setUsername(review.getUser() != null ? review.getUser().getUsername() : null);
        dto.setClubId(review.getClub() != null ? review.getClub().getClubId() : null);
        dto.setRating(review.getRating());
        dto.setDescription(review.getDescription());
        dto.setCreatedAt(review.getCreatedAtOffset());
        dto.setFromTableGuest(review.isFromTableGuest());
        dto.setPhotoId(review.getPhotoId());

        return dto;
    }

    public static ReviewDto toDto(final Review review, final UriInfo uriInfo) {
        ReviewDto dto = toDto(review);
        if (dto == null || uriInfo == null) {
            return dto;
        }

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/reviews/{reviewId}")
                .resolveTemplate("clubId", dto.getClubId())
                .resolveTemplate("reviewId", dto.getReviewId())
                .build(), "GET"));
        links.add(LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", dto.getClubId())
                .build(), "GET"));
        if (dto.getUserId() != null) {
            links.add(LinkBuilder.link("user", uriInfo.getBaseUriBuilder()
                    .path("users/{userId}")
                    .resolveTemplate("userId", dto.getUserId())
                    .build(), "GET"));
        }
        if (dto.getPhotoId() != null) {
            String photoUrl = uriInfo.getBaseUriBuilder()
                    .path("photos/{photoId}")
                    .resolveTemplate("photoId", dto.getPhotoId())
                    .build()
                    .toString();
            dto.setPhotoUrl(photoUrl);
            links.add(LinkBuilder.link("photo", photoUrl, "GET"));
        }
        dto.setLinks(links);

        return dto;
    }
}
