package ar.edu.itba.paw.model.dto;

public class ClubCreateRequestDto {
    private String clubName;
    private String location;
    private String priceRange; // BAJO, MEDIO, ALTO
    private String description;
    private String musicGenre; // ROCK, POP, etc.
    private String zone; // ZONA_NORTE, etc.
    private boolean isBar;
    private String cbu;
//    private String imageUrl; // opcional

    public ClubCreateRequestDto() {}

    public String getClubName() { return clubName; }
    public void setClubName(String clubName) { this.clubName = clubName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriceRange() { return priceRange; }
    public void setPriceRange(String priceRange) { this.priceRange = priceRange; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMusicGenre() { return musicGenre; }
    public void setMusicGenre(String musicGenre) { this.musicGenre = musicGenre; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public boolean isBar() { return isBar; }
    public void setBar(boolean bar) { isBar = bar; }

    public String getCbu() { return cbu; }
    public void setCbu(String cbu) { this.cbu = cbu; }

//    public String getImageUrl() { return imageUrl; }
//    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}