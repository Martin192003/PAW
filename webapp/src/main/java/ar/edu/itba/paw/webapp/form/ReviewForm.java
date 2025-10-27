package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ReviewForm {
    @NotBlank(message = "{reviews.comment.required}")
    @Size(max = 1000, message = "{reviews.comment.max}")
    private String description;

    private Integer rating;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
}
