package ar.edu.itba.paw.model;

import ar.edu.itba.paw.model.converter.MusicalGenreConverter;
import ar.edu.itba.paw.model.converter.PriceConverter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;


@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "club_id", updatable = false, nullable = false)
    private UUID clubId;

    @Column(name = "club_name", nullable = false, length = 100)
    private String clubName;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(precision = 3, scale = 1) // 9.9 fits; your 2,1 was too tight
    private BigDecimal rating;

    @Column(name = "quantity_ratings")
    private Integer quantityRatings;

    @Convert(converter = PriceConverter.class)
    @Column(name = "price_range", length = 20)
    private Price priceRange;

    @Column
    private String description;

    // If you want ONE contact: store a FK to User (ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_info")
    private User contactInfo;

    @Convert(converter = MusicalGenreConverter.class)
    @Column(name = "music_genre", length = 50)
    private MusicalGenre musicGenre;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", length = 50)
    private Zone zone;

    @Column(name = "is_bar", nullable = false)
    private Boolean isBar;

    @ManyToMany
    @JoinTable(
            name = "club_admins",
            joinColumns = @JoinColumn(name = "club_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> admins = new HashSet<>();

    public Club() { }

    public Club(UUID clubId, String clubName, String location, java.math.BigDecimal rating,
                Integer quantityRatings, Price priceRange, String description,
                User contactInfo, MusicalGenre musicGenre, OffsetDateTime createdAt, Zone zone, Boolean isBar) {
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
        this.isBar = isBar;
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

    public Boolean getIsBar() { return isBar; }
    public void setIsBar(Boolean isBar) { this.isBar = isBar; }

    public Set<User> getAdmins() { return admins; }
    public void setAdmins(Set<User> admins) { this.admins = admins; }
}
