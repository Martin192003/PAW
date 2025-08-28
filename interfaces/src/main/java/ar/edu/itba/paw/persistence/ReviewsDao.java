package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.UUID;

public interface ReviewsDao {
    Review findById(UUID id);
    List<Review> findAll();
    List<Review> findByUser(User user);
    List<Review> findByClub(Club club);

    Review create(User user, Club club, int rating, String description);
    Review update(Review review);
    boolean delete(UUID id);
}
