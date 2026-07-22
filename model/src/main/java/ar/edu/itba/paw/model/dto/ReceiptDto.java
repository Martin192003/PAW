package ar.edu.itba.paw.model.dto;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ReceiptDto {
    private UUID id;
    private UUID bookingId;
    private UUID uploadedBy;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private OffsetDateTime createdAt;
    private List<LinkDto> links;

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(UUID uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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
