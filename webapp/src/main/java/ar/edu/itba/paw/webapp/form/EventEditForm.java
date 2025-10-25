package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.ValidEventEditDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

@ValidEventEditDate
public class EventEditForm {
    @NotNull
    private UUID clubId;

    private UUID eventId;

    @NotBlank(message = "{event.name.notblank}")
    @Size(max = 120)
    private String eventName;

    @NotBlank(message = "{event.description.notnull}")
    @Size(max = 2000)
    private String description;


    @NotNull(message = "{event.name.notblank}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;


    private UUID photoIdId;
    private MultipartFile photoId;


    // getters/setters
    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public MultipartFile getPhotoId() { return photoId; }
    public void setPhotoId(MultipartFile photoId) { this.photoId = photoId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getPhotoIdId() { return photoIdId; }
    public void setPhotoIdId(UUID photoIdId) { this.photoIdId = photoIdId; }
}