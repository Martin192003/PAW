package ar.edu.itba.paw.model.dto;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class PhotoDto {
    private UUID photoId;
    private UUID clubId;
    private UUID postedBy;
    private UUID reviewId;
    private String contentType;
    private Long sizeBytes;
    private String photoType;
    private OffsetDateTime createdAt;
    private List<LinkDto> links;

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getPhotoId() {
        return photoId;
    }

    public void setPhotoId(UUID photoId) {
        this.photoId = photoId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(UUID postedBy) {
        this.postedBy = postedBy;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getReviewId() {
        return reviewId;
    }

    public void setReviewId(UUID reviewId) {
        this.reviewId = reviewId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getPhotoType() {
        return photoType;
    }

    public void setPhotoType(String photoType) {
        this.photoType = photoType;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
