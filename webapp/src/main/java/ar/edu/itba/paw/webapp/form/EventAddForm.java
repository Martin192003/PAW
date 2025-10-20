package ar.edu.itba.paw.webapp.form;

import org.springframework.format.annotation.DateTimeFormat;


import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;


public class EventAddForm {
    @NotNull
    private UUID clubId;


    @NotBlank @Size(max = 120)
    private String eventName;


    @Size(max = 2000)
    private String description;


    @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;


    private UUID photoId; // opcional


    // getters/setters
    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public UUID getPhotoId() { return photoId; }
    public void setPhotoId(UUID photoId) { this.photoId = photoId; }
}