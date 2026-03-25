package ar.edu.itba.paw.model.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class EventDto {

    private UUID id;
    private String eventName;

    private LocalDate date; // JSON: "2026-02-01"

    private String description;
    private Map<String,String> links;

    private UUID photoId;

    public EventDto() {}

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, String> getLinks() {
        return links;
    }
    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public  void setPhotoId(UUID photoId) { this.photoId = photoId; }
    public UUID getPhotoId() { return photoId; }

}
