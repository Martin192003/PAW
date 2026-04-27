package ar.edu.itba.paw.model.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class BookingDto {
    private UUID id;
    private UUID clubId;
    private UUID userId;
    private UUID tableId;
    private OffsetDateTime bookingStart;
    private OffsetDateTime bookingEnd;
    private String status;
    private OffsetDateTime createdAt;
    private BigDecimal totalAmount;
    private Map<String, String> links;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public OffsetDateTime getBookingStart() {
        return bookingStart;
    }

    public void setBookingStart(OffsetDateTime bookingStart) {
        this.bookingStart = bookingStart;
    }

    public OffsetDateTime getBookingEnd() {
        return bookingEnd;
    }

    public void setBookingEnd(OffsetDateTime bookingEnd) {
        this.bookingEnd = bookingEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }
}
