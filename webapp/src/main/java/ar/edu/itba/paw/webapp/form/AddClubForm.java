package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Price;
import ar.edu.itba.paw.model.Zone;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.UUID;

public class AddClubForm {

    private UUID clubId;

    @NotBlank(message = "{club.name.notblank}")
    private String name;

    @NotBlank(message = "{club.location.notblank}")
    private String location;

    @NotNull(message = "{club.priceRange.notnull}")
    private Price priceRange;

    @NotBlank(message = "{club.description.notblank}")
    private String description;

    @NotNull(message = "{club.genre.notnull}")
    private MusicalGenre musicGenre;

    @NotNull(message = "{club.zone.notnull}")
    private Zone zone; // Enum con ZONA_CABA, ZONA_NORTE, ZONA_OESTE, ZONA_SUR

    @NotNull(message = "{club.image.notnull}")
    private MultipartFile image;

    // --- Getters y Setters ---
    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Price getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(Price priceRange) {
        this.priceRange = priceRange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MusicalGenre getMusicGenre() {
        return musicGenre;
    }

    public void setMusicGenre(MusicalGenre musicGenre) {
        this.musicGenre = musicGenre;
    }

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }
}

