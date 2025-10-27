package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Review;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewService {
    double getAverageRating(UUID clubId);
    Review createReview(UUID userId, UUID clubId, int rating, String description, MultipartFile photo) throws IOException;
    Optional<Review> getUserReviewForClub(UUID userId, UUID clubId);
    List<Review> getReviewsByClub(UUID clubId);
    }