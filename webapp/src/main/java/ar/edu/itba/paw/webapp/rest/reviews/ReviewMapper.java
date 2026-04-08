package ar.edu.itba.paw.webapp.rest.reviews;

import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.dto.ReviewDto;

public final class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewDto toDto(final Review review) {
        if (review == null) return null;

        ReviewDto dto = new ReviewDto();
        dto.setReviewId(review.getReviewId());
        dto.setUserId(review.getUser() != null ? review.getUser().getId() : null);
        dto.setClubId(review.getClub() != null ? review.getClub().getClubId() : null);
        dto.setRating(review.getRating());
        dto.setDescription(review.getDescription());
        dto.setCreatedAt(review.getCreatedAtOffset());
        dto.setFromTableGuest(review.isFromTableGuest());

        return dto;
    }
}