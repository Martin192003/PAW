package ar.edu.itba.paw.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class BookingDto {
    private UUID id;
    private UUID clubId;
    private UUID userId;
    private UUID tableId;
    private LocalDate date;
    private OffsetDateTime bookingStart;
    private OffsetDateTime bookingEnd;
    private String status;
    private OffsetDateTime createdAt;
    private BigDecimal totalAmount;
    private boolean hasReceipt;
    private List<LinkDto> links;

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

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public boolean isHasReceipt() {
        return hasReceipt;
    }

    public void setHasReceipt(boolean hasReceipt) {
        this.hasReceipt = hasReceipt;
    }

    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
