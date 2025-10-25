package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.ValidEventDate;
import ar.edu.itba.paw.webapp.form.validation.ValidImage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;


import javax.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

@ValidEventDate
public class EventAddForm {
    @NotNull
    private UUID clubId;

    @NotBlank(message = "{contact.name.notblank}")
    @Size(max = 120, message = "{contact.name.size}")
    private String eventName;

    @NotBlank(message = "{product.description.notblank}")
    @Size(max = 2000, message = "{contact.description.size}")
    private String description;


    @NotNull(message = "{event.date.notnull}")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @ValidImage
    private MultipartFile photoId;

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
}