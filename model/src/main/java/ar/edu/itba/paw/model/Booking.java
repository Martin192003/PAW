package ar.edu.itba.paw.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public class Booking {

    private UUID bookingId;
    private User user;
    private UUID table;
    private OffsetDateTime bookingStart;
    private OffsetDateTime bookingEnd;
    private Status status; // 'confirmada' | 'pendiente' | 'cancelada'
    private OffsetDateTime createdAt;
    private BigDecimal totalAmount;

    public Booking() {}

    public Booking(UUID bookingId, User user, UUID table, OffsetDateTime bookingStart,
                   OffsetDateTime bookingEnd, Status status, OffsetDateTime createdAt,
                   BigDecimal totalAmount) {
        this.bookingId = bookingId;
        this.user = user;
        this.table = table;
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
        this.status = status;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
    }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public UUID getTableId() { return table; }
    public void setTableId(UUID tableId) { this.table= tableId; }

    public OffsetDateTime getBookingStart() { return bookingStart; }
    public void setBookingStart(OffsetDateTime bookingStart) { this.bookingStart = bookingStart; }

    public OffsetDateTime getBookingEnd() { return bookingEnd; }
    public void setBookingEnd(OffsetDateTime bookingEnd) { this.bookingEnd = bookingEnd; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
