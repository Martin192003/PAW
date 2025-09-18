package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {

    private ReviewsDao reviewsDao;
    private ClubDao clubDao;

    public ReviewServiceImpl(ReviewsDao reviewsDao, ClubDao clubDao) {
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
}
