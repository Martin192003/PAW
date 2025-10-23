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
import java.util.*;

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

    @Mock
    private UserService userService;

    @Mock
    private PhotoService photoService;
    @Mock
    private ClubTablesDao tablesDao;
    @Mock
    private EmailService emailService;

    @Test
    public void testGetClubByIdFound() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null, null);

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
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null,  null);
        User contact = Mockito.mock(User.class);
        UUID contactId = UUID.randomUUID();
        Mockito.when(contact.getId()).thenReturn(contactId);
        Mockito.when(userService.findById(contactId)).thenReturn(contact);
        Mockito.when(contact.getRole()).thenReturn(Role.USER);
        Mockito.when(contact.getEmail()).thenReturn("test@mail.com");
        Mockito.when(mockDao.create(CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, true, null)).thenReturn(club);
        Club result = clubService.create(CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE, contact, MUSIC_GENRE, Zone.ZONA_CABA, true, null,null);
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_NAME, result.getClubName());
    }

    @Test
    public void testAdvancedSearch() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null, null);
        Mockito.when(mockDao.search("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name")).thenReturn(List.of(club));
        List<Club> result = clubService.advancedSearch("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(CLUB_NAME, result.getFirst().getClubName());
    }

    @Test
    public void testUpdateClubById() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null, null);
        Mockito.when(mockDao.update(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, Zone.ZONA_CABA, true, null)).thenReturn(club);
        Club result = clubService.update(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, Zone.ZONA_CABA, true, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_ID, result.getClubId());
    }

    @Test
    public void testUpdateClub() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, true, null);
        Mockito.when(mockDao.update(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, Zone.ZONA_CABA, true, null)).thenReturn(club);
        Club result = clubService.updateClub(club);
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_ID, result.getClubId());
    }

    @Test
    public void testGetAdminsByClub() {
        User user = Mockito.mock(User.class);
        Mockito.when(mockDao.findAdminsByClubId(CLUB_ID)).thenReturn(Collections.singletonList(user));
        List<User> result = clubService.getAdminsByClub(CLUB_ID);
        Assert.assertEquals(1, result.size());
    }


    @Test
    public void testSearchPaginated() {
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null, null);
        Mockito.when(mockDao.searchPaginated("Club", Zone.ZONA_CABA, "ROCK", "MEDIO", true, "name", 1, 0)).thenReturn(List.of(club));
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
        Club club = new Club(CLUB_ID, CLUB_NAME, LOCATION, RATING, QUANTITY_RATINGS, PRICE_RANGE, DESCRIPTION, null, MUSIC_GENRE, CREATED_AT, Zone.ZONA_CABA, null, null);
        ClubTable table = Mockito.mock(ClubTable.class);
        Mockito.when(table.getClubId()).thenReturn(CLUB_ID);
        Mockito.when(tablesDao.findById(CLUB_ID)).thenReturn(Optional.of(table));
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        ClubServiceImpl service = new ClubServiceImpl(clubDao, null, tablesDao, null,userService);
        Map<UUID, String> result = service.mapClubNames(List.of(booking));
        Assert.assertEquals(CLUB_NAME, result.get(CLUB_ID));
    }
}