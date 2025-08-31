package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public class Review {

    private UUID reviewId;
    private User user;
    private Club club;
    private Integer rating; // 1..5 (validado por la DB)
    private String description;
    private OffsetDateTime createdAt;

    public Review() {}

    public Review(UUID reviewId, User user, Club club, Integer rating,
                  String description, OffsetDateTime createdAt) {
        this.reviewId = reviewId;
        this.user = user;
        this.club = club;
        this.rating = rating;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getReviewId() { return reviewId; }
    public void setReviewId(UUID reviewId) { this.reviewId = reviewId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
