package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
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
import java.util.Map;
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

    @Mock
    private EmailServiceImpl mailService;

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

    @Test
    public void testCreateClub() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        User contact = Mockito.mock(User.class);
        Mockito.when(contact.getEmail()).thenReturn("test@mail.com");
        Mockito.when(mockDao.create(CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, true)).thenReturn(club);
        Club result = clubService.create(CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_NAME, result.getClubName());
    }

    @Test
    public void testAdvancedSearch() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Mockito.when(mockDao.search("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name")).thenReturn(Arrays.asList(club));
        List<Club> result = clubService.advancedSearch("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(CLUB_NAME, result.get(0).getClubName());
    }

    @Test
    public void testUpdateClubById() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Mockito.when(mockDao.update(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, Zone.ZONA_CABA, true)).thenReturn(club);
        Club result = clubService.update(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, Zone.ZONA_CABA, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_ID, result.getClubId());
    }

    @Test
    public void testUpdateClub() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, true);
        Mockito.when(mockDao.update(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, Zone.ZONA_CABA, true)).thenReturn(club);
        Club result = clubService.updateClub(club);
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_ID, result.getClubId());
    }

    @Test
    public void testGetClubsByUser() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Mockito.when(mockDao.findByUserId(CLUB_ID)).thenReturn(Arrays.asList(club));
        List<Club> result = clubService.getClubsByUser(CLUB_ID);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(CLUB_ID, result.get(0).getClubId());
    }

    @Test
    public void testGetAdminsByClub() {
        User user = Mockito.mock(User.class);
        Mockito.when(mockDao.findAdminsByClubId(CLUB_ID)).thenReturn(Arrays.asList(user));
        List<User> result = clubService.getAdminsByClub(CLUB_ID);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testGetAllClubsPaginated() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Mockito.when(mockDao.findAllPaginated(1, 0)).thenReturn(Arrays.asList(club));
        List<Club> result = clubService.getAllClubsPaginated(1, 0);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testCountAllClubs() {
        Mockito.when(mockDao.countAll()).thenReturn(5);
        int result = clubService.countAllClubs();
        Assert.assertEquals(5, result);
    }

    @Test
    public void testGetClubsByUserPaginated() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Mockito.when(mockDao.findByUserIdPaginated(CLUB_ID, 1, 0)).thenReturn(Arrays.asList(club));
        List<Club> result = clubService.getClubsByUserPaginated(CLUB_ID, 1, 0);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testCountClubsByUser() {
        Mockito.when(mockDao.countByUserId(CLUB_ID)).thenReturn(3);
        int result = clubService.countClubsByUser(CLUB_ID);
        Assert.assertEquals(3, result);
    }

    @Test
    public void testSearchPaginated() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Mockito.when(mockDao.searchPaginated("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name", 1, 0)).thenReturn(Arrays.asList(club));
        List<Club> result = clubService.searchPaginated("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name", 1, 0);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testCountSearch() {
        Mockito.when(mockDao.countSearch("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true)).thenReturn(2);
        int result = clubService.countSearch("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true);
        Assert.assertEquals(2, result);
    }

    @Test
    public void testMapClubNames() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(booking.getTableId()).thenReturn(CLUB_ID);
        Mockito.when(booking.getBookingId()).thenReturn(CLUB_ID);
        ClubTablesDao tablesDao = Mockito.mock(ClubTablesDao.class);
        ClubDao clubDao = Mockito.mock(ClubDao.class);
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null);
        Table table = Mockito.mock(Table.class);
        Mockito.when(table.getClubId()).thenReturn(CLUB_ID);
        Mockito.when(tablesDao.findById(CLUB_ID)).thenReturn(Optional.of(table));
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        ClubServiceImpl service = new ClubServiceImpl(clubDao, null, tablesDao);
        Map<UUID, String> result = service.mapClubNames(Arrays.asList(booking));
        Assert.assertEquals(CLUB_NAME, result.get(CLUB_ID));
    }
}