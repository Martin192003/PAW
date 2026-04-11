package ar.edu.itba.paw.model.dto;

public class ReviewCreateRequestDto {

    private Integer rating;
    private String description;
    private boolean fromTableGuest;

    public ReviewCreateRequestDto() {}

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isFromTableGuest() {
        return fromTableGuest;
    }

    public void setFromTableGuest(boolean fromTableGuest) {
        this.fromTableGuest = fromTableGuest;
    }
}