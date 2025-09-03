package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Photo {

    private UUID photoId;
    private UUID clubId;
    private UUID postedBy;
    private OffsetDateTime createdAt;

    private String filename;
    private String contentType;
    private Long sizeBytes;
    private String url;

    // Solo cuando realmente necesites enviar el binario
    private byte[] data;

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

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}