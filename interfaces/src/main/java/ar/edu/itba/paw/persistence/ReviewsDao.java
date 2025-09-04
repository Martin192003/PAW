package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewsDao {
    Optional<Review> findById(UUID id);
    List<Review> findAll();
    List<Review> findByUser(User user);
    List<Review> findByClub(Club club);

    Review create(User user, Club club, int rating, String description);
    Review update(UUID reviewId, int rating, String description);
    Review upsert(User user, Club club, int rating, String description);
    boolean delete(UUID id);
}
