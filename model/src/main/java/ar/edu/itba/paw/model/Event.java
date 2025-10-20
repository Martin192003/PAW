package ar.edu.itba.paw.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table
public class Event {

    @Id
    private  UUID eventId;

    @Column
    private  UUID clubId;

    @Column
    private  String eventName;

    @Column
    private String description;

    @Column
    private LocalDate date;

    @Column
    private  UUID photoId;


    public Event(UUID eventId, UUID clubId, String eventName, String description, LocalDate date, UUID photoId) {
        this.eventId = eventId;
        this.clubId = clubId;
        this.eventName = eventName;
        this.description = description;
        this.date = date;
        this.photoId = photoId;
    }

    public Event() {

    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public UUID getPhotoId() {
        return photoId;
    }

    public void setPhotoId(UUID photoId) {
        this.photoId = photoId;
    }
}
