package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EventDto {

    private UUID id;
    private String eventName;

    private LocalDate date; // JSON: "2026-02-01"

    private String description;
    private List<LinkDto> links;

    private UUID photoId;
    private String clubName;

    public EventDto() {}

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    // Esta clase no declara @XmlAccessorType, asi que MOXy usa el default
    // PUBLIC_MEMBER (mira los getters/setters, no los campos), por eso el
    // adapter va aca y no en el field. Sin esto, MOXy usa el patron de
    // datetime global de JaxRsConfig (eclipselink.json.date-format) tambien
    // para esta fecha sin hora, y el body "2026-07-22" que manda el
    // <input type="date"> del frontend falla al deserializar (500 crudo,
    // antes de llegar al controller/service).
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
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

    public List<LinkDto> getLinks() {
        return links;
    }
    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }

    public  void setPhotoId(UUID photoId) { this.photoId = photoId; }
    public UUID getPhotoId() { return photoId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

}
