package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public class Activity {

    private UUID activityId;
    private Booking booking;
    private String activityType;
    private OffsetDateTime activityDate;
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
