package ar.edu.itba.paw.model.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ClubDto {
    private UUID clubId;
    private String clubName;
    private String description;
    private String location;
    private String zone;
    private String musicGenre;
    private String priceRange;
    private boolean isBar;
    private Float rating;
    private Integer quantityRatings;
    private List<LinkDto> links;
    private OffsetDateTime createdAt;
//    private UserDto contactInfo;

    public ClubDto() {}

    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getMusicGenre() { return musicGenre; }
    public void setMusicGenre(String musicGenre) { this.musicGenre = musicGenre; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public boolean isBar() { return isBar; }
    public void setBar(boolean bar) { isBar = bar; }

    public Float getRating() { return rating; }
    public void setRating(Float rating) { this.rating = rating; }

    public Integer getQuantityRatings() { return quantityRatings; }
    public void setQuantityRatings(Integer quantityRatings) { this.quantityRatings = quantityRatings; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public List<LinkDto> getLinks() { return links; }
    public void setLinks(List<LinkDto> links) { this.links = links; }

//    public UserDto getContactInfo() { return contactInfo; }
//    public void setContactInfo(UserDto contactInfo) { this.contactInfo = contactInfo; }
}

