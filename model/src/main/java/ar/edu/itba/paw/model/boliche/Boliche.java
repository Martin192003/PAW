package ar.edu.itba.paw.model.boliche;


import ar.edu.itba.paw.model.common.Contact;
import ar.edu.itba.paw.model.common.Location;

import java.util.HashSet;
import java.util.Set;

public class Boliche {
    private final long id;
    private String name;
    private final VenueType type;
    private Location location;
    private Contact contact;
    private Set<MusicalGenre> musicalGenres = new HashSet<>();
    private Price price;
    private double rating;
    private int quantityRatings;
    private String description;


    public Boliche(long id, VenueType type, String name, Location location, Contact contact, Price price, double rating, int quantityRatings, String description) {

        /* ACA DEBERIAN HABER VALIDACIONES */
        this.id = id;
        this.type = type;
        this.name = name;
        this.location = location;
        this.contact = contact;
        this.price = price;
        this.rating = rating;
        this.quantityRatings = quantityRatings;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name;}
    public VenueType getType() { return type;}
    public Location getLocation() { return location; }
    public Contact getContact() { return contact; }
    public Set<MusicalGenre> getMusicalGenres() { return musicalGenres;}
    public Price getPrice() { return price; }
    public double getRating() { return rating; }
    public int getQuantityRatings() { return quantityRatings; }
    public String getDescription() { return description; }

    public void setLocation(Location location) { this.location = location; }
    public void setContact(Contact contact) { this.contact = contact; }
    public void setMusicalGenres(Set<MusicalGenre> musicalGenres) { this.musicalGenres = musicalGenres; }
    public void setPrice(Price price) { this.price = price; }
    public void setRating(double rating) { this.rating = rating;}
    public void setQuantityRatings(int quantityRatings) { this.quantityRatings = quantityRatings;}
    public void setDescription(String description) { this.description = description;}
    public void setName(String name) { this.name = name; }
}
