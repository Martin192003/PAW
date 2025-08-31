package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public class Photo {

    private UUID photoId;
    private Club club;
    private String url;
    private User postedBy;
    private OffsetDateTime createdAt;

    public Photo() {}

    public Photo(UUID photoId, Club club, String url, User postedBy, OffsetDateTime createdAt) {
        this.photoId = photoId;
        this.club = club;
        this.url = url;
        this.postedBy = postedBy;
        this.createdAt = createdAt;
    }

    public UUID getPhotoId() { return photoId; }
    public void setPhotoId(UUID photoId) { this.photoId = photoId; }

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public User getPostedBy() { return postedBy; }
    public void setPostedBy(User postedBy) { this.postedBy = postedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
