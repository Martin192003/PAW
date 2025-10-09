package ar.edu.itba.paw.model;

import javax.persistence.*;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @Column(name = "receipt_id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID receiptId;

    @Column(name = "booking_id", columnDefinition = "UUID", nullable = false)
    private UUID bookingId;

    @Column(name = "uploaded_by", columnDefinition = "UUID", nullable = false)
    private UUID uploadedBy;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data", nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public Receipt() {}

    public Receipt(UUID receiptId, UUID bookingId, UUID uploadedBy, String url, String filename,
                   String contentType, Long sizeBytes, OffsetDateTime createdAt, byte[] data) {
        this.receiptId = receiptId;
        this.bookingId = bookingId;
        this.uploadedBy = uploadedBy;
        this.url = url;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
        this.data = data;
    }

    @PrePersist
    private void prePersist() {
        if (receiptId == null) receiptId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (sizeBytes == null && data != null) sizeBytes = (long) data.length;
        if (url == null) url = "/receipts/" + receiptId;
    }

    public UUID getReceiptId() { return receiptId; }
    public void setReceiptId(UUID receiptId) { this.receiptId = receiptId; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public UUID getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(UUID uploadedBy) { this.uploadedBy = uploadedBy; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
