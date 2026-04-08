package ar.edu.itba.paw.model;

import javax.persistence.*;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reviews_user_club", columnNames = {"user_id","club_id"})
        }
)
public class Review {

    @Id
    @Column(name = "review_id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "from_table_guest")
    private boolean fromTableGuest;

    @Column(name = "photo_id")
    private UUID photoId;

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

    @PrePersist
    private void prePersist() {
        if (reviewId == null) reviewId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
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

    public Date getCreatedAt() { return createdAt == null ? null : Date.from(createdAt.toInstant()); }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isFromTableGuest() { return fromTableGuest; }
    public void setFromTableGuest(boolean fromTableGuest) { this.fromTableGuest = fromTableGuest; }

    public UUID getPhotoId() { return photoId; }
    public void setPhotoId(UUID photoId) { this.photoId = photoId; }

    public OffsetDateTime getCreatedAtOffset() {return createdAt;}
}
