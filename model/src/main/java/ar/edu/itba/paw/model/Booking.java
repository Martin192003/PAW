package ar.edu.itba.paw.model;

import ar.edu.itba.paw.model.converter.StatusConverter;

import javax.persistence.*;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Convert;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @Column(name = "booking_id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", referencedColumnName = "table_id", nullable = false)
    private ClubTable table;

    @Column(name = "table_id", columnDefinition = "UUID", insertable = false, updatable = false)
    private UUID tableId;

    @Column(name = "booking_start", nullable = false)
    private OffsetDateTime bookingStart;

    @Column(name = "booking_end", nullable = false)
    private OffsetDateTime bookingEnd;

    @Convert(converter = StatusConverter.class) //ESTO ESTA RARO PREGUNTAR
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    public Booking() {}

    public Booking(UUID bookingId, User user, ClubTable table, OffsetDateTime bookingStart,
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

    public ClubTable getTable() { return table; }
    public void setTable(ClubTable table) { this.table = table; }

    public void setTableId(UUID tableId) { this.tableId = tableId; }
    public UUID getTableId() { return tableId; }
}
