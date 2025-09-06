package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
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
    private static final String PRICE_RANGE = "medio";
    private static final String DESCRIPTION = "Descripción";
    private static final String MUSIC_GENRE = "Rock";
    private static final Zone CLUB_ZONE = Zone.ZONA_CABA;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now();

    @Mock
    private ClubDao mockDao;

    @InjectMocks
    private ClubServiceImpl clubService;

    @Test
    public void testGetAllClubs() {
        Club club1 = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, CLUB_ZONE);
        Club club2 = new Club(UUID.randomUUID(), "OtroClub", "OtraUbicación", RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, CLUB_ZONE);
        Mockito.when(mockDao.findAll()).thenReturn(List.of(club1, club2));

        List<Club> clubs = clubService.getAllClubs();

        Assert.assertNotNull(clubs);
        Assert.assertEquals(2, clubs.size());
        Assert.assertEquals(CLUB_NAME, clubs.get(0).getClubName());
        Assert.assertEquals("OtroClub", clubs.get(1).getClubName());
    }

    @Test
    public void testGetClubByIdFound() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, CLUB_ZONE);
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

    @Test
    public void testAdvancedSearchOnlyName() {
        Club club1 = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, CLUB_ZONE);
        Mockito.when(mockDao.search(CLUB_NAME, null, null, null, null, null)).thenReturn(List.of(club1));

        List<Club> result = clubService.advancedSearch(CLUB_NAME, null, null, null, null, null);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(CLUB_NAME, result.get(0).getClubName());
    }

    @Test
    public void testAdvancedSearchByLocationAndGenre() {
        Club club1 = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, CLUB_ZONE);
        Mockito.when(mockDao.search(null, CLUB_ZONE, MUSIC_GENRE, null, null, null)).thenReturn(List.of(club1));

        List<Club> result = clubService.advancedSearch(null, CLUB_ZONE, MUSIC_GENRE, null, null, null);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(CLUB_NAME, result.get(0).getClubName());
    }

    @Test
    public void testAdvancedSearch() {
        Club club1 = new Club(CLUB_ID, CLUB_NAME, LOCATION, BigDecimal.valueOf(4.0), QUANTITY_RATINGS, "bajo", DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, CLUB_ZONE);
        Mockito.when(mockDao.search(CLUB_NAME, CLUB_ZONE, MUSIC_GENRE, "bajo", BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.5)))
                .thenReturn(List.of(club1));

        List<Club> result = clubService.advancedSearch(CLUB_NAME, CLUB_ZONE, MUSIC_GENRE, "bajo", BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.5));

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(CLUB_NAME, result.get(0).getClubName());
    }

    @Test
    public void testAdvancedSearchNoResults() {
        Mockito.when(mockDao.search("NonExistent", null, null, null, null, null)).thenReturn(List.of());

        List<Club> result = clubService.advancedSearch("NonExistent", null, null, null, null, null);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }
}