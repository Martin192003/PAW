package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.InvalidClubException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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
    public void createReview(UUID userId, UUID clubId, int rating, String description, MultipartFile photo) throws IOException {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Club club = clubDao.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));


        boolean hadTable = bookingDao.existsPastConfirmedByUserAndClub(userId, clubId, OffsetDateTime.now());
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Review review = reviewsDao.upsert(user, club, rating, description, hadTable);
        updateClubRating(club);
        if (photo != null && !photo.isEmpty()) {
            UUID photoId = photoService.uploadReviewPhoto(clubId, review.getReviewId(), user.getId(), photo);
            if (photoId != null) {
                reviewsDao.updatePhotoId(review.getReviewId(), photoId);
            }
        }

        // Notificar al dueño del club si corresponde
        if (hadTable) {
            final UUID ownerId = club.getContactInfo() != null ? club.getContactInfo().getId() : null;
            if (ownerId != null) {

                emailService.sendOwnerReviewAlert(club, review, Locale.getDefault());
            }
        }
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

    @Transactional(readOnly = true)
    public List<Review> listByClub(UUID clubId, String sort, int limit, int offset) {
        return reviewsDao.searchPaginated(null, clubId, sort, limit, offset);
    }

    @Transactional(readOnly = true)
    public int countByClub(UUID clubId) {
        return reviewsDao.countSearch(null, clubId);
    }

    @Override
    @Transactional
    public Review upsertReview(UUID userId, UUID clubId, int rating, String description, Locale locale) {
        final User user = userDao.findById(userId).orElseThrow();
        final Club club = clubDao.findById(clubId).orElseThrow();

        final boolean hadTable = true;

        Review review = reviewsDao.upsert(user, club, rating, description, hadTable);

        // setear tag si decidiste persistirlo:
        if (review != null && hasFromTableGuestField(review)) { // helper para no romper si no agregaste la columna
            review.setFromTableGuest(hadTable);
        }

        // Notificar al dueño del club si corresponde
//        if (hadTable) {
//            final UUID ownerId = club.getContactInfo() != null ? club.getContactInfo().getId() : null; // adaptar a tu modelo
//            if (ownerId != null) {
//                notificationService.notifyNewTableGuestReview(ownerId, clubId, review.getReviewId(), rating);
//
//                // Enviar mail DESDE EL SERVICE (regla de negocio) y con el locale del destinatario si lo tenés guardado,
//                // no el del cliente que crea la review.  :contentReference[oaicite:6]{index=6} :contentReference[oaicite:7]{index=7}
//                mailService.sendOwnerReviewAlert(ownerId, clubId, review.getReviewId(), locale);
//            }
//        }

        return review;
    }

    private boolean hasFromTableGuestField(Review review) {
        try {
            review.getClass().getDeclaredField("fromTableGuest");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<Review> listByUser(UUID userId, String sort, int limit, int offset) {
        return reviewsDao.searchPaginated(userId, null, sort, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByUser(UUID userId) {
        return reviewsDao.countSearch(userId, null);
    }
}
