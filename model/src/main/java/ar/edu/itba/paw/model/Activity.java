package ar.edu.itba.paw.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity")
public class Activity {

    @Id
    @Column(name = "activity_id", columnDefinition = "UUID", nullable = false, updatable = false)
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    private UUID activityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "activity_date", nullable = false)
    private OffsetDateTime activityDate;

    @Column(name = "details")
    private String details;

    public Activity() {}

    public Activity(UUID activityId, Booking booking, String activityType,
                    OffsetDateTime activityDate, String details) {
        this.activityId = activityId;
        this.booking = booking;
        this.activityType = activityType;
        this.activityDate = activityDate;
        this.details = details;
    }

    public UUID getActivityId() { return activityId; }
    public void setActivityId(UUID activityId) { this.activityId = activityId; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public OffsetDateTime getActivityDate() { return activityDate; }
    public void setActivityDate(OffsetDateTime activityDate) { this.activityDate = activityDate; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
