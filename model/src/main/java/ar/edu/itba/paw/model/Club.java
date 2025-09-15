package ar.edu.itba.paw.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.math.BigDecimal;

public class Club {
    private UUID clubId;
    private String clubName;
    private String location;
    private BigDecimal rating;
    private Integer quantityRatings;
    private Price priceRange; // 'bajo' | 'medio' | 'alto'
    private String description;
    private User contactInfo;
    private MusicalGenre musicGenre;
    private OffsetDateTime createdAt;
    private Zone zone;

    public Club() {}

    public Club(UUID clubId, String clubName, String location, java.math.BigDecimal rating,
                Integer quantityRatings, Price priceRange, String description,
                User contactInfo, MusicalGenre musicGenre, OffsetDateTime createdAt, Zone zone) {
        this.clubId = clubId;
        this.clubName = clubName;
        this.location = location;
        this.rating = rating;
        this.quantityRatings = quantityRatings;
        this.priceRange = priceRange;
        this.description = description;
        this.contactInfo = contactInfo;
        this.musicGenre = musicGenre;
        this.createdAt = createdAt;
        this.zone = zone;
    }

    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public java.math.BigDecimal getRating() { return rating; }
    public void setRating(java.math.BigDecimal rating) { this.rating = rating; }

    public Integer getQuantityRatings() { return quantityRatings; }
    public void setQuantityRatings(Integer quantityRatings) { this.quantityRatings = quantityRatings; }

    public Price getPriceRange() { return priceRange; }
    public void setPriceRange(Price priceRange) { this.priceRange = priceRange; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getContactInfo() { return contactInfo; }
    public void setContactInfo(User contactInfo) { this.contactInfo = contactInfo; }

    public MusicalGenre getMusicGenre() { return musicGenre; }
    public void setMusicGenre(MusicalGenre musicGenre) { this.musicGenre = musicGenre; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }
}
