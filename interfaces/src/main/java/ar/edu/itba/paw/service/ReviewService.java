package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public interface ReviewService {
    double getAverageRating(UUID clubId);

    Review upsertReview(UUID userId, UUID clubId, int rating, String description, Locale locale);

    @Transactional
    void updateClubRating(Club club);

    void createReview(UUID userId, UUID clubId, int rating, String description, MultipartFile photo) throws IOException;
    List<Review> listByClub(UUID clubId, String sort, int limit, int offset);
    int countByClub(UUID clubId);
    Optional<Review> getUserReviewForClub(UUID userId, UUID clubId);
    List<Review> getReviewsByClub(UUID clubId);
    }