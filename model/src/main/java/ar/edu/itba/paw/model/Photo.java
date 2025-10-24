package ar.edu.itba.paw.model;

import javax.persistence.*;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @Column(name = "photo_id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID photoId;

    @Column(name = "club_id", columnDefinition = "UUID", nullable = false)
    private UUID clubId;

    @Column(name = "posted_by", columnDefinition = "UUID", nullable = false)
    private UUID postedBy;


    @Column(name = "review_id", columnDefinition = "uuid")
    private UUID reviewId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "url", nullable = false)
    private String url;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Enumerated(EnumType.STRING)
    @Column(name = "photo_type")
    private PhotoType photoType = PhotoType.CAROUSEL;

    @PrePersist
    private void beforeInsert() {
        if (this.photoId == null) this.photoId = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = OffsetDateTime.now();
        if (this.sizeBytes == null && this.data != null) this.sizeBytes = (long) this.data.length;
        if (this.url == null) this.url = "/photos/" + this.photoId;
    }

    public UUID getPhotoId() { return photoId; }
    public void setPhotoId(UUID photoId) { this.photoId = photoId; }

    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

    public UUID getPostedBy() { return postedBy; }
    public void setPostedBy(UUID postedBy) { this.postedBy = postedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public UUID getReviewId() { return reviewId; }
    public void setReviewId(UUID reviewId) { this.reviewId = reviewId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public PhotoType getPhotoType() { return photoType; }
    public void setPhotoType(PhotoType photoType) { this.photoType = photoType;}
}