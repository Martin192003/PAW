package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Price;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.persistence.ClubDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ClubServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final String CLUB_NAME = "ClubTest";
    private static final String LOCATION = "Ubicación";
    private static final BigDecimal RATING = BigDecimal.valueOf(4.5);
    private static final Integer QUANTITY_RATINGS = 10;
    private static final Price PRICE_RANGE = Price.MEDIO;
    private static final String DESCRIPTION = "Descripción";
    private static final MusicalGenre MUSIC_GENRE = MusicalGenre.ROCK;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now();

    @Mock
    private ClubDao mockDao;

    @InjectMocks
    private ClubServiceImpl clubService;

    @Test
    public void testGetAllClubs() {
        Club club1 = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Club club2 = new Club(UUID.randomUUID(), "OtroClub", "OtraUbicación", RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);

        Mockito.when(mockDao.findAll()).thenReturn(Arrays.asList(club1, club2));

        List<Club> clubs = clubService.getAllClubs();

        Assert.assertNotNull(clubs);
        Assert.assertEquals(2, clubs.size());
        Assert.assertEquals(CLUB_NAME, clubs.get(0).getClubName());
        Assert.assertEquals("OtroClub", clubs.get(1).getClubName());
    }

    @Test
    public void testGetClubByIdFound() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);

        Mockito.when(mockDao.findById(CLUB_ID)).thenReturn(Optional.of(club));

        Optional<Club> result = clubService.getClubById(CLUB_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(CLUB_NAME, result.get().getClubName());
    }

    @Test
    public void testGetClubByIdNotFound() {
        Mockito.when(mockDao.findById(CLUB_ID)).thenReturn(Optional.empty());

        Optional<Club> result = clubService.getClubById(CLUB_ID);

        Assert.assertFalse(result.isPresent());
    }
}