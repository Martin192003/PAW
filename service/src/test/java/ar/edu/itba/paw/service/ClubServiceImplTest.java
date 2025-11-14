package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class ClubServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CLUB_NAME = "La Noche";
    private static final String DESCRIPTION = "Club nocturno premium";
    private static final String LOCATION = "Palermo";
    private static final Price PRICE_RANGE = Price.MEDIO;
    private static final MusicalGenre MUSIC_GENRE = MusicalGenre.ELECTRONICA;
    private static final Zone ZONE = Zone.ZONA_CABA;
    private static final Boolean IS_BAR = false;
    private static final String CBU = "1234567890123456789012";
    private static final String EMAIL = "admin@club.com";

    @InjectMocks
    private ClubServiceImpl clubService;

    @Mock
    private ClubDao clubDao;

    @Mock
    private EmailService mailService;

    @Mock
    private PhotoService photoService;

    @Mock
    private UserService userService;

    @Mock
    private BookingDao bookingDao;

    private User mockUser;
    private Club mockClub;
    private Photo mockPhoto;

    @Before
    public void setUp() {
        mockUser = new User();
        mockUser.setUserId(USER_ID);
        mockUser.setEmail(EMAIL);
        mockUser.setRole(Role.USER);

        mockClub = new Club();
        mockClub.setClubId(CLUB_ID);
        mockClub.setClubName(CLUB_NAME);
        mockClub.setDescription(DESCRIPTION);
        mockClub.setLocation(LOCATION);
        mockClub.setPriceRange(PRICE_RANGE);
        mockClub.setContactInfo(mockUser);
        mockClub.setMusicGenre(MUSIC_GENRE);
        mockClub.setZone(ZONE);
        mockClub.setIsBar(IS_BAR);
        mockClub.setCbu(CBU);

        mockPhoto = new Photo();
        mockPhoto.setFilename("cover.jpg");
        mockPhoto.setContentType("image/jpeg");
        mockPhoto.setData(new byte[]{1, 2, 3});
    }

    // ========================
    // CREATE - HAPPY PATH
    // ========================

    @Test
    public void testCreate_Success_WithImage() {
        // 1. Setup
        Mockito.when(userService.findById(Mockito.eq(USER_ID)))
                .thenReturn(Optional.of(mockUser));
        Mockito.when(clubDao.create(
                Mockito.eq(CLUB_NAME),
                Mockito.eq(DESCRIPTION),
                Mockito.eq(LOCATION),
                Mockito.eq(PRICE_RANGE),
                Mockito.eq(mockUser),
                Mockito.eq(MUSIC_GENRE),
                Mockito.eq(ZONE),
                Mockito.eq(IS_BAR),
                Mockito.eq(CBU)
        )).thenReturn(mockClub);

        // 2. Exercise
        Club result = clubService.create(
                CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE,
                mockUser, MUSIC_GENRE, ZONE, IS_BAR, CBU, mockPhoto
        );

        // 3. Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_ID, result.getClubId());
        Assert.assertEquals(CLUB_NAME, result.getClubName());
        Assert.assertEquals(Role.ADMIN, mockUser.getRole());
    }

    @Test
    public void testCreate_Success_WithoutImage() {
        // 1. Setup
        Mockito.when(userService.findById(Mockito.eq(USER_ID)))
                .thenReturn(Optional.of(mockUser));
        Mockito.when(clubDao.create(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()
        )).thenReturn(mockClub);

        // 2. Exercise
        Club result = clubService.create(
                CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE,
                mockUser, MUSIC_GENRE, ZONE, IS_BAR, CBU, null
        );

        // 3. Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(CLUB_ID, result.getClubId());
    }

    @Test
    public void testCreate_UserNotFound() {
        // 1. Setup
        Mockito.when(userService.findById(Mockito.eq(USER_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise + 3. Assert
        Assert.assertThrows(RuntimeException.class, () -> {
            clubService.create(CLUB_NAME, DESCRIPTION, LOCATION, PRICE_RANGE,
                    mockUser, MUSIC_GENRE, ZONE, IS_BAR, CBU, null);
        });
    }

    // ========================
    // GET CLUB BY ID
    // ========================

    @Test
    public void testGetClubById_Exists() {
        // 1. Setup
        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));

        // 2. Exercise
        Optional<Club> result = clubService.getClubById(CLUB_ID);

        // 3. Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(mockClub, result.get());
    }

    @Test
    public void testGetClubById_NotExists() {
        // 1. Setup
        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise
        Optional<Club> result = clubService.getClubById(CLUB_ID);

        // 3. Assert
        Assert.assertFalse(result.isPresent());
    }

    // ========================
    // UPDATE
    // ========================

    @Test
    public void testUpdate_Success() {
        // 1. Setup
        Club updatedClub = new Club();
        updatedClub.setClubId(CLUB_ID);
        updatedClub.setClubName("La Noche VIP");

        Mockito.when(clubDao.update(
                Mockito.eq(CLUB_ID),
                Mockito.eq("La Noche VIP"),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any()
        )).thenReturn(updatedClub);

        // 2. Exercise
        Club result = clubService.update(
                CLUB_ID, "La Noche VIP", LOCATION, BigDecimal.TEN, 5,
                PRICE_RANGE, DESCRIPTION, USER_ID, MUSIC_GENRE, ZONE, IS_BAR, CBU
        );

        // 3. Assert
        Assert.assertEquals("La Noche VIP", result.getClubName());
    }

    // ========================
    // GET CLUBS BY USER
    // ========================

    @Test
    public void testGetClubsByUser() {
        // 1. Setup
        List<Club> clubs = Arrays.asList(mockClub);
        Mockito.when(clubDao.findByUserId(Mockito.eq(USER_ID)))
                .thenReturn(clubs);

        // 2. Exercise
        List<Club> result = clubService.getClubsByUser(USER_ID);

        // 3. Assert
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockClub));
    }

    // ========================
    // GET ADMINS BY CLUB
    // ========================

    @Test
    public void testGetAdminsByClub() {
        // 1. Setup
        List<User> admins = Arrays.asList(mockUser);
        Mockito.when(clubDao.findAdminsByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(admins);

        // 2. Exercise
        List<User> result = clubService.getAdminsByClub(CLUB_ID);

        // 3. Assert
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockUser));
    }

    // ========================
    // ASSERT ADMIN
    // ========================

    @Test
    public void testAssertAdmin_Success() {
        // 1. Setup
        Mockito.when(clubDao.findAdminsByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Arrays.asList(mockUser));

        // 2. Exercise & 3. Assert (no exception)
        clubService.assertAdmin(CLUB_ID, USER_ID);
    }

    @Test
    public void testAssertAdmin_Fail() {
        // 1. Setup
        User otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        Mockito.when(clubDao.findAdminsByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Arrays.asList(otherUser));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(UnauthorizedException.class, () -> {
            clubService.assertAdmin(CLUB_ID, USER_ID);
        });
    }

    // ========================
    // SEND CONTACT MESSAGE
    // ========================

    @Test
    public void testSendContactMessage_Success_WithContactEmail() {
        // 1. Setup
        mockClub.setContactInfo(mockUser);
        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));

        // 2. Exercise
        boolean result = clubService.sendContactMessage(
                CLUB_ID, "user@mail.com", "Juan", "123456", "Hola"
        );

        // 3. Assert
        Assert.assertTrue(result);
    }

    @Test
    public void testSendContactMessage_Success_FallbackEmail() {
        // 1. Setup
        mockClub.setContactInfo(null);
        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));

        // 2. Exercise
        boolean result = clubService.sendContactMessage(
                CLUB_ID, "user@mail.com", "Juan", "123456", "Hola"
        );

        // 3. Assert
        Assert.assertTrue(result);
    }

    @Test
    public void testSendContactMessage_ClubNotFound() {
        // 1. Setup
        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise
        boolean result = clubService.sendContactMessage(
                CLUB_ID, "user@mail.com", "Juan", "123456", "Hola"
        );

        // 3. Assert
        Assert.assertFalse(result);
    }

    // ========================
    // IS USER OWNER
    // ========================

    @Test
    public void testIsUserOwner_Admin() {
        // 1. Setup
        mockUser.setRole(Role.ADMIN);

        // 2. Exercise
        boolean result = clubService.isUserOwner(CLUB_ID, mockUser);

        // 3. Assert
        Assert.assertTrue(result);
    }

    @Test
    public void testIsUserOwner_IsAdminOfClub() {
        // 1. Setup
        mockUser.setRole(Role.USER);
        Mockito.when(clubDao.findAdminsByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Arrays.asList(mockUser));

        // 2. Exercise
        boolean result = clubService.isUserOwner(CLUB_ID, mockUser);

        // 3. Assert
        Assert.assertTrue(result);
    }

    @Test
    public void testIsUserOwner_NotOwner() {
        // 1. Setup
        mockUser.setRole(Role.USER);
        Mockito.when(clubDao.findAdminsByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Arrays.asList());

        // 2. Exercise
        boolean result = clubService.isUserOwner(CLUB_ID, mockUser);

        // 3. Assert
        Assert.assertFalse(result);
    }

    @Test
    public void testIsUserOwner_NullUser() {
        // 1. Exercise
        boolean result = clubService.isUserOwner(CLUB_ID, null);

        // 3. Assert
        Assert.assertFalse(result);
    }

    // ========================
    // SEARCH PAGINATED
    // ========================

    @Test
    public void testSearchPaginated() {
        // 1. Setup
        List<Club> clubs = Arrays.asList(mockClub);
        Mockito.when(clubDao.searchPaginated(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.anyInt()
        )).thenReturn(clubs);

        // 2. Exercise
        List<Club> result = clubService.searchPaginated(
                "Noche", Zone.ZONA_CABA, "ELECTRONIC", "MEDIUM", false, "name", 10, 0
        );

        // 3. Assert
        Assert.assertEquals(1, result.size());
    }

    // ========================
    // MAP CLUB NAMES
    // ========================

    @Test
    public void testMapClubNames() {
        // 1. Setup
        Booking b1 = new Booking();
        b1.setBookingId(UUID.randomUUID());
        List<Booking> bookings = Arrays.asList(b1);

        Mockito.when(bookingDao.mapClubNamesForBookings(Mockito.anyList()))
                .thenReturn(Collections.singletonMap(b1.getBookingId(), "La Noche"));

        // 2. Exercise
        Map<UUID, String> result = clubService.mapClubNames(bookings);

        // 3. Assert
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("La Noche", result.get(b1.getBookingId()));
    }
}