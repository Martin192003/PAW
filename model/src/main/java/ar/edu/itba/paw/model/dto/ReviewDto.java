package ar.edu.itba.paw.model.dto;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ReviewDto {

    private UUID reviewId;
    private UUID userId;
    private String username;
    private UUID clubId;
    private String clubName;
    private String clubLocation;
    private Integer rating;
    private String description;
    private OffsetDateTime createdAt;
    private boolean fromTableGuest;
    private UUID photoId;
    private String photoUrl;
    private List<LinkDto> links;

    public ReviewDto() {
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getReviewId() {
        return reviewId;
    }

    public void setReviewId(UUID reviewId) {
        this.reviewId = reviewId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getClubLocation() {
        return clubLocation;
    }

    public void setClubLocation(String clubLocation) {
        this.clubLocation = clubLocation;
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

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public boolean isFromTableGuest() {
        return fromTableGuest;
    }

    public void setFromTableGuest(boolean fromTableGuest) {
        this.fromTableGuest = fromTableGuest;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getPhotoId() {
        return photoId;
    }

    public void setPhotoId(UUID photoId) {
        this.photoId = photoId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
