package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.InvalidClubException;
import ar.edu.itba.paw.model.exceptions.ReviewNotFounException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewsDao reviewsDao;
    private final PhotoService photoService;
    private final ClubDao clubDao;
    private final UserDao userDao;
    private final BookingDao bookingDao;
    private final EmailService emailService;

    public ReviewServiceImpl(ReviewsDao reviewsDao, PhotoService photoService, ClubDao clubDao, UserDao userDao, BookingDao bookingDao, EmailService emailService) {
        this.photoService = photoService;
        this.userDao = userDao;
        this.reviewsDao = reviewsDao;
        this.clubDao = clubDao;
        this.bookingDao = bookingDao;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageRating(UUID clubId) {
        clubDao.findById(clubId).orElseThrow(InvalidClubException::new);
        return reviewsDao.averageByClubId(clubId);
    }

    @Transactional
    @Override
    public void updateClubRating(Club club) {
        double avg = getAverageRating(club.getClubId());
        club.setRating(BigDecimal.valueOf(avg));
        club.setQuantityRatings(reviewsDao.findByClub(club).size());
        clubDao.update(
                club.getClubId(),
                club.getClubName(),
                club.getLocation(),
                club.getRating(),
                club.getQuantityRatings(),
                club.getPriceRange(),
                club.getDescription(),
                club.getContactInfo() != null ? club.getContactInfo().getId() : null,
                club.getMusicGenre(),
                club.getZone(),
                club.getIsBar(),
                club.getCbu()
        );
    }

    @Override
    @Transactional
    public void createReview(UUID userId, UUID clubId, int rating, String description, Photo photo) throws IOException {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Club club = clubDao.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        boolean hadTable = bookingDao.existsPastConfirmedByUserAndClub(userId, clubId, OffsetDateTime.now());
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        UUID previousPhotoId = reviewsDao.findByUserAndClub(user, club)
                .map(Review::getPhotoId)
                .orElse(null);

        Review review = reviewsDao.upsert(user, club, rating, description, hadTable);
        updateClubRating(club);
        if (photo != null) {
            UUID photoId = photoService.uploadReviewPhoto(clubId, review.getReviewId(), user.getId(), photo);
            if (photoId != null) {
                reviewsDao.updatePhotoId(review.getReviewId(), photoId);
                review.setPhotoId(photoId);
                deleteReplacedReviewPhoto(previousPhotoId, photoId);
            }
        }

        if (hadTable) {
            final UUID ownerId = club.getContactInfo() != null ? club.getContactInfo().getId() : null;
            if (ownerId != null) {

                emailService.sendOwnerReviewAlert(club, review, Locale.getDefault());
            }
        }
    }

    @Override
    @Transactional
    public Review updateReview(UUID reviewId, int rating, String description, Photo photo) throws IOException {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review existing = reviewsDao.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFounException("No se encontro la review"));
        Club club = existing.getClub();
        UUID previousPhotoId = existing.getPhotoId();

        Review updated = reviewsDao.update(reviewId, rating, description);
        if (updated == null) {
            throw new ReviewNotFounException("No se encontro la review");
        }

        updateClubRating(club);
        if (photo != null) {
            UUID photoId = photoService.uploadReviewPhoto(
                    club.getClubId(),
                    updated.getReviewId(),
                    updated.getUser().getId(),
                    photo
            );
            if (photoId != null) {
                reviewsDao.updatePhotoId(updated.getReviewId(), photoId);
                updated.setPhotoId(photoId);
                deleteReplacedReviewPhoto(previousPhotoId, photoId);
            }
        }

        return updated;
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
    public Optional<Review> findById(UUID reviewId) {
        return reviewsDao.findById(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> listByClub(UUID clubId, String sort, int limit, int offset) {
        final String safeSort = (sort == null) ? "" : sort;
        return reviewsDao.searchPaginated(null, clubId, safeSort, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> listByClubPaginated(UUID clubId, String sort,int limit, int offset){
        return listByClub(clubId, sort, limit, getCorrectedOffset(clubId, limit, offset));
    }

    @Override
    @Transactional(readOnly = true)
    public int countByClub(UUID clubId) {
        return reviewsDao.countSearch(null, clubId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Review> listByUser(UUID userId, String sort, int limit, int offset) {
        final String safeSort = (sort == null) ? "" : sort;
        return reviewsDao.searchPaginated(userId, null, safeSort, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByUser(UUID userId) {
        return reviewsDao.countSearch(userId, null);
    }



    @Override
    @Transactional
    public void deleteReview(UUID reviewId) {

        final Review review = reviewsDao.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFounException("No se encontro la review"));

        final UUID clubId  = review.getClub().getClubId();
        final UUID photoId = review.getPhotoId();

        final boolean deleted = reviewsDao.delete(reviewId);

        reviewsDao.flush();

        boolean photoDeleted = true;
        if (photoId != null) {
            try {
                photoDeleted = photoService.delete(photoId);
            } catch (Exception e) {
                photoDeleted = false;
            }
        }

        if (deleted) {
            recalcClubRating(clubId);
        }

    }

    /** Recalcula rating y cantidad desde DB y persiste solo esas columnas. */
    private void recalcClubRating(UUID clubId) {
        final int count = reviewsDao.countSearch(null, clubId);
        if (count == 0) {
            clubDao.updateRatingAndCount(clubId, BigDecimal.ONE, 0);
            return;
        }

        final Double avgOrNull = reviewsDao.averageByClubId(clubId);
        final double avg = (avgOrNull != null) ? avgOrNull : 0.0;

        final BigDecimal rounded = BigDecimal
                .valueOf(avg)
                .setScale(1, java.math.RoundingMode.HALF_UP);

        clubDao.updateRatingAndCount(clubId, rounded, count);
    }

    private void deleteReplacedReviewPhoto(UUID previousPhotoId, UUID newPhotoId) {
        if (previousPhotoId == null || previousPhotoId.equals(newPhotoId)) {
            return;
        }
        photoService.delete(previousPhotoId);
    }

    public List<Review> findByClubPaginated(UUID clubId, int page, int size) {
        return reviewsDao.findByClubPaginated(clubId, page, size);
    }

    public int countByClubPaginated(UUID clubId) {
        return reviewsDao.countByClubPaginated(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCorrectedOffset(UUID clubId, int limit, int offset) {
        int totalCount = countByClub(clubId);
        if (offset >= totalCount && totalCount > 0) {
            int lastPage = (int) Math.floor((totalCount - 1) / (double) limit);
            return lastPage * limit;
        }
        return offset;
    }

    @Override
    public int calculateOffset(int offset, int limit, int total) {

        if (total <= 0) {
            return 0;
        }

        if (offset >= total) {
            int lastPage = (int) Math.floor((total - 1) / (double) limit);

            return lastPage * limit;
        }

        return offset;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> obtainPaginatedReviews(UUID userId, String sort, int limit, int offset) {

        final int total = countByUser(userId);

        final int offsetCorregido = calculateOffset(offset, limit, total);

        if (total == 0) {
            return Collections.emptyList();
        }

        final String safeSort = (sort == null) ? "" : sort;
        return listByUser(userId, safeSort, limit, offsetCorregido);
    }

    @Override
    @Transactional(readOnly = true)
    public int gettotalReviewsPages(UUID clubId, int size) {
        if (size <= 0) {
            return 1;
        }

        int totalReviews = countByClubPaginated(clubId);
        return (int) Math.ceil((double) totalReviews / size);
    }
}
