package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.InvalidClubException;
import ar.edu.itba.paw.model.exceptions.ReviewNotFounException;
import ar.edu.itba.paw.persistence.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class ReviewServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now();

    @Mock private ReviewsDao reviewsDao;
    @Mock private ClubDao clubDao;
    @Mock private UserDao userDao;
    @Mock private BookingDao bookingDao;

    @InjectMocks private ReviewServiceImpl reviewService;

    @Test
    public void testGetAverageRatingReturnsValue() {
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(new Club()));
        Mockito.when(reviewsDao.averageByClubId(CLUB_ID)).thenReturn(4.5);

        double avg = reviewService.getAverageRating(CLUB_ID);

        Assert.assertEquals(4.5, avg, 0.01);
    }

    @Test(expected = InvalidClubException.class)
    public void testGetAverageRatingClubNotFound() {
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.empty());
        reviewService.getAverageRating(CLUB_ID);
    }

    @Test
    public void testCreateReviewSuccess() throws IOException {
        User user = new User(USER_ID);
        Club club = new Club();
        club.setClubId(CLUB_ID);

        Review expectedReview = new Review(REVIEW_ID, user, club, 5, "Great!", CREATED_AT);

        Mockito.when(userDao.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(bookingDao.existsPastConfirmedByUserAndClub(Mockito.eq(USER_ID), Mockito.eq(CLUB_ID), Mockito.any())).thenReturn(true);
        Mockito.when(reviewsDao.upsert(user, club, 5, "Great!", true)).thenReturn(expectedReview);
        Mockito.when(reviewsDao.averageByClubId(CLUB_ID)).thenReturn(4.0);
        Mockito.when(reviewsDao.findByClub(club)).thenReturn(List.of(expectedReview));

        reviewService.createReview(USER_ID, CLUB_ID, 5, "Great!", null);

        Assert.assertEquals(BigDecimal.valueOf(4.0), club.getRating());
        Assert.assertEquals(1, (int) club.getQuantityRatings());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateReviewInvalidRating() throws IOException {
        User user = new User(USER_ID);
        Club club = new Club();
        club.setClubId(CLUB_ID);

        Mockito.when(userDao.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));

        reviewService.createReview(USER_ID, CLUB_ID, 0, "Bad", null);
    }

    @Test
    public void testGetUserReviewForClubReturnsReview() {
        User user = new User(USER_ID);
        Club club = new Club();
        club.setClubId(CLUB_ID);
        Review review = new Review(REVIEW_ID, user, club, 4, "Nice", CREATED_AT);

        Mockito.when(userDao.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(reviewsDao.findByUserAndClub(user, club)).thenReturn(Optional.of(review));

        Optional<Review> result = reviewService.getUserReviewForClub(USER_ID, CLUB_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(review.getReviewId(), result.get().getReviewId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUserReviewForClubUserNotFound() {
        Mockito.when(userDao.findById(USER_ID)).thenReturn(Optional.empty());
        reviewService.getUserReviewForClub(USER_ID, CLUB_ID);
    }

    @Test(expected = ReviewNotFounException.class)
    public void testDeleteReviewNotFound() {
        Mockito.when(reviewsDao.findById(REVIEW_ID)).thenReturn(Optional.empty());
        reviewService.deleteReview(REVIEW_ID);
    }

    @Test
    public void testDeleteReviewSuccess() {
        User user = new User(USER_ID);
        Club club = new Club();
        club.setClubId(CLUB_ID);
        Review review = new Review(REVIEW_ID, user, club, 4, "Nice", CREATED_AT);

        Mockito.when(reviewsDao.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        Mockito.when(reviewsDao.delete(REVIEW_ID)).thenReturn(true);
        Mockito.when(reviewsDao.averageByClubId(CLUB_ID)).thenReturn(3.5);
        Mockito.when(reviewsDao.countSearch(null, CLUB_ID)).thenReturn(1);

        reviewService.deleteReview(REVIEW_ID);

        Assert.assertTrue(true);
    }

    @Test
    public void testDeleteLastReviewKeepsPersistedRatingCompatibleWithSchema() {
        User user = new User(USER_ID);
        Club club = new Club();
        club.setClubId(CLUB_ID);
        Review review = new Review(REVIEW_ID, user, club, 4, "Nice", CREATED_AT);

        Mockito.when(reviewsDao.findById(REVIEW_ID)).thenReturn(Optional.of(review));
        Mockito.when(reviewsDao.delete(REVIEW_ID)).thenReturn(true);
        Mockito.when(reviewsDao.countSearch(null, CLUB_ID)).thenReturn(0);

        reviewService.deleteReview(REVIEW_ID);

        Mockito.verify(clubDao).updateRatingAndCount(CLUB_ID, BigDecimal.ONE, 0);
        Mockito.verify(reviewsDao, Mockito.never()).averageByClubId(CLUB_ID);
    }

    @Test
    public void testUpdateClubRating() {
        Club club = new Club();
        club.setClubId(CLUB_ID);
        club.setClubName("ClubTest");

        Review sample = new Review(REVIEW_ID, new User(USER_ID), club, 4, "desc", CREATED_AT);

        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(reviewsDao.averageByClubId(CLUB_ID)).thenReturn(4.0);
        Mockito.when(reviewsDao.findByClub(club)).thenReturn(List.of(sample));

        reviewService.updateClubRating(club);

        Assert.assertEquals(BigDecimal.valueOf(4.0), club.getRating());
        Assert.assertEquals(1, (int) club.getQuantityRatings());
    }

    @Test
    public void testListByClub() {
        List<Review> reviews = List.of(new Review(REVIEW_ID, new User(USER_ID), new Club(), 5, "desc", CREATED_AT));
        Mockito.when(reviewsDao.searchPaginated(null, CLUB_ID, "rating", 10, 0)).thenReturn(reviews);

        List<Review> result = reviewService.listByClub(CLUB_ID, "rating", 10, 0);

        Assert.assertEquals(reviews, result);
    }

    @Test
    public void testCountByClub() {
        Mockito.when(reviewsDao.countSearch(null, CLUB_ID)).thenReturn(3);
        Assert.assertEquals(3, reviewService.countByClub(CLUB_ID));
    }

    @Test
    public void testListByUser() {
        List<Review> reviews = List.of(new Review(REVIEW_ID, new User(USER_ID), new Club(), 5, "desc", CREATED_AT));
        Mockito.when(reviewsDao.searchPaginated(USER_ID, null, "rating", 10, 0)).thenReturn(reviews);

        List<Review> result = reviewService.listByUser(USER_ID, "rating", 10, 0);

        Assert.assertEquals(reviews, result);
    }

    @Test
    public void testCountByUser() {
        Mockito.when(reviewsDao.countSearch(USER_ID, null)).thenReturn(2);
        Assert.assertEquals(2, reviewService.countByUser(USER_ID));
    }

    @Test
    public void testGetCorrectedOffset() {

        Mockito.when(reviewsDao.countSearch(null, CLUB_ID)).thenReturn(25);
        Assert.assertEquals(20, reviewService.getCorrectedOffset(CLUB_ID, 10, 30));
    }


    @Test
    public void testFindByClubPaginated() {
        List<Review> reviews = List.of(new Review(REVIEW_ID, new User(USER_ID), new Club(), 5, "desc", CREATED_AT));
        Mockito.when(reviewsDao.findByClubPaginated(CLUB_ID, 1, 10)).thenReturn(reviews);

        List<Review> result = reviewService.findByClubPaginated(CLUB_ID, 1, 10);

        Assert.assertEquals(reviews, result);
    }

    @Test
    public void testCountByClubPaginated() {
        Mockito.when(reviewsDao.countByClubPaginated(CLUB_ID)).thenReturn(7);
        Assert.assertEquals(7, reviewService.countByClubPaginated(CLUB_ID));
    }
}
