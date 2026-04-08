package ar.edu.itba.paw.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewDto {

    private UUID reviewId;
    private UUID userId;
    private UUID clubId;
    private Integer rating;
    private String description;
    private OffsetDateTime createdAt;
    private boolean fromTableGuest;

    public ReviewDto() {
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public void setReviewId(UUID reviewId) {
        this.reviewId = reviewId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFromTableGuest() {
        return fromTableGuest;
    }

    public void setFromTableGuest(boolean fromTableGuest) {
        this.fromTableGuest = fromTableGuest;
    }
}