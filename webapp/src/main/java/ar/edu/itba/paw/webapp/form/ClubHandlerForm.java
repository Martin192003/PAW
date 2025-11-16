package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.UUID;

public class ClubHandlerForm {
    private String editedField;
    private UUID clubId;
    private String image;

    @NotEmpty(message = "{clubHandlerForm.name.notEmpty}")
    @Size(max = 100, message = "{error.name}")
    private String name;

    @NotEmpty(message = "{clubHandlerForm.location.notEmpty}")
    @Size(max = 255, message = "{error.location}")
    private String location;

    @NotEmpty(message = "{clubHandlerForm.zone.notEmpty}")
    private String zone;

    @NotEmpty(message = "{clubHandlerForm.musicgenre.notEmpty}")
    private String musicGenre;

    @NotEmpty(message = "{clubHandlerForm.price.notEmpty}")
    private String priceRange;

    @NotBlank(message = "{clubHandlerForm.description.notEmpty}")
    private String description;

    @Size(max = 50, message = "{clubHandlerForm.cbu.size}")
    @NotBlank(message = "{club.cbu.notnull}")
    private String cbu;

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getMusicGenre() { return musicGenre; }
    public void setMusicGenre(String musicGenre) { this.musicGenre = musicGenre; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

    public String getEditedField() { return editedField; }
    public void setEditedField(String editedField) { this.editedField = editedField; }

    public String getCbu() { return cbu; }
    public void setCbu(String cbu) { this.cbu = cbu; }
}
