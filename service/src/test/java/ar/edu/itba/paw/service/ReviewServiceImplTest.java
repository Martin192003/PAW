package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ReviewServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final String CLUB_NAME = "ClubTest";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now();

    @Mock
    private ReviewsDao reviewsDao;

    @Mock
    private ClubDao clubDao;

    @InjectMocks
    private ReviewServiceImpl reviewService = new ReviewServiceImpl(reviewsDao, clubDao);

    @Test
    public void testGetAverageRatingWithReviews() {
        Club club = new Club(CLUB_ID, CLUB_NAME, "Ubicación", null, null, "medio", "desc", null, "Rock", CREATED_AT, null);
        Review review1 = new Review(UUID.randomUUID(), null, club, 4, "Muy bueno", CREATED_AT);
        Review review2 = new Review(UUID.randomUUID(), null, club, 2, "Regular", CREATED_AT);

        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(reviewsDao.findByClub(club)).thenReturn(Arrays.asList(review1, review2));

        double avg = reviewService.getAverageRating(CLUB_ID);

        Assert.assertEquals(3.0, avg, 0.01);
    }

    @Test
    public void testGetAverageRatingNoReviews() {
        Club club = new Club(CLUB_ID, CLUB_NAME, "Ubicación", null, null, "medio", "desc", null, "Rock", CREATED_AT, null);

        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(reviewsDao.findByClub(club)).thenReturn(Collections.emptyList());

        double avg = reviewService.getAverageRating(CLUB_ID);

        Assert.assertEquals(0.0, avg, 0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAverageRatingClubNotFound() {
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.empty());
        reviewService.getAverageRating(CLUB_ID);
    }
}