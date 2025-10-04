package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class Receipt {
    private UUID receiptId;
    private UUID bookingId;
    private UUID uploadedBy;
    private String url;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private OffsetDateTime createdAt;
    private byte[] data;


    public Receipt(UUID receiptId, UUID bookingId, UUID uploadedBy, String url, String filename, String contentType, Long sizeBytes, OffsetDateTime createdAt, byte[] data) {
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

    public Receipt() {}

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