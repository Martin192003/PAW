package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private ReviewsDao reviewsDao;
    private ClubDao clubDao;
    private UserDao userDao;

    public ReviewServiceImpl(ReviewsDao reviewsDao, ClubDao clubDao, UserDao userDao) {
        this.userDao = userDao;
        this.reviewsDao = reviewsDao;
        this.clubDao = clubDao;
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageRating(UUID clubId) {
        List<Review> reviews = reviewsDao.findByClub(clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new));
        if (reviews.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Review review : reviews) {
            sum += review.getRating();
        }
        return sum / reviews.size();
    }

    @Override
    @Transactional
    public Review createReview(UUID userId, UUID clubId, int rating, String description) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Club club = clubDao.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        return reviewsDao.upsert(user, club, rating, description);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> getUserReviewForClub(UUID userId, UUID clubId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Club club = clubDao.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        return reviewsDao.findByUserAndClub(user, club);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getReviewsByClub(UUID clubId) {
        Club club = clubDao.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));
        return reviewsDao.findByClub(club);
    }
}
