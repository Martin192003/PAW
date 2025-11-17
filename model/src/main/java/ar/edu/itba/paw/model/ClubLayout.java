package ar.edu.itba.paw.model;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "club_layouts", indexes = {
        @Index(name = "idx_club_layouts_club_id", columnList = "club_id")
})
public class ClubLayout {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "layout_id", nullable = false, updatable = false)
    private UUID layoutId;

    @Column(name = "club_id", nullable = false, unique = true)
    private UUID clubId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "url", nullable = false)
    private String url;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "data", columnDefinition = "bytea")
    private byte[] data;

    @Column(name = "filename")
    private String filename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ClubLayout() { }

    public ClubLayout(UUID clubId, UUID uploadedBy, String url, byte[] data, String filename, String contentType, Long sizeBytes) {
        this.clubId = clubId;
        this.uploadedBy = uploadedBy;
        this.url = url;
        this.data = data;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = OffsetDateTime.now();
    }

    @PrePersist
    private void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.url == null && this.layoutId != null && this.clubId != null) {
            this.url = "/clubs/club/" + this.clubId + "/layout/" + this.layoutId + "/image";
        }
    }

    public UUID getLayoutId() { return layoutId; }
    public void setLayoutId(UUID layoutId) { this.layoutId = layoutId; }

    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}





