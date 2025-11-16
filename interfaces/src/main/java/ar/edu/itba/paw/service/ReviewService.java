package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Review;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewService {
    double getAverageRating(UUID clubId);

    void updateClubRating(Club club);

    void createReview(UUID userId, UUID clubId, int rating, String description, Photo photo) throws IOException;
    List<Review> listByClub(UUID clubId, String sort, int limit, int offset);
    int countByClub(UUID clubId);
    Optional<Review> getUserReviewForClub(UUID userId, UUID clubId);
    List<Review> listByUser(UUID userId, String sort, int limit, int offset);
    int countByUser(UUID userId);
    void deleteReview(UUID reviewId);
    List<Review> findByClubPaginated(UUID clubId, int page, int size);
    int countByClubPaginated(UUID clubId);
    int getCorrectedOffset(UUID clubId,int limit, int offset);
    int calculateOffset(int offset, int limit, int total);
    List<Review> obtainPaginatedReviews(UUID userId, String sort, int limit, int offset);
    int gettotalReviewsPages(UUID clubId, int size);
}