package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.EventDao;
import ar.edu.itba.paw.persistence.PhotoDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class PhotoServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final String FILENAME = "cover.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final byte[] DATA = new byte[]{1, 2, 3};
    private static final String URL = "/api/photos/" + PHOTO_ID;

    @InjectMocks
    private PhotoServiceImpl photoService;

    @Mock
    private PhotoDao photoDao;

    @Mock
    private UserService userService;

    @Mock
    private EventDao eventDao;

    private Photo mockPhoto;
    private User mockUser;

    @Before
    public void setUp() {
        mockUser = new User();
        mockUser.setUserId(USER_ID);
        mockUser.setEmail("user@club.com");

        mockPhoto = new Photo();
        mockPhoto.setPhotoId(PHOTO_ID);
        mockPhoto.setClubId(CLUB_ID);
        mockPhoto.setPostedBy(USER_ID);
        mockPhoto.setFilename(FILENAME);
        mockPhoto.setContentType(CONTENT_TYPE);
        mockPhoto.setData(DATA);
        mockPhoto.setUrl(URL);
    }

    // ========================
    // UPLOAD - HAPPY PATH
    // ========================

    @Test
    public void testUpload_Success() {
        // 1. Setup
        Mockito.when(photoDao.save(Mockito.any(Photo.class)))
                .thenReturn(PHOTO_ID);

        // 2. Exercise
        UUID result = photoService.upload(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);

        // 3. Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(PHOTO_ID, result);
    }

    // ========================
    // UPLOAD COVER - HAPPY PATH
    // ========================

    @Test
    public void testUploadCover_Success() {
        // 1. Setup
        Mockito.doNothing().when(photoDao).deleteCoverByClubId(Mockito.eq(CLUB_ID));
        Mockito.when(photoDao.save(Mockito.any(Photo.class)))
                .thenReturn(PHOTO_ID);

        // 2. Exercise
        photoService.uploadCover(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);

        // 3. Assert: No exception thrown
    }

    // ========================
    // UPLOAD CAROUSEL - HAPPY PATH
    // ========================

    @Test
    public void testUploadCarousel_Success() {
        // 1. Setup
        Mockito.when(photoDao.countByClubIdAndType(Mockito.eq(CLUB_ID), Mockito.eq(PhotoType.CAROUSEL)))
                .thenReturn(19L);
        Mockito.when(photoDao.save(Mockito.any(Photo.class)))
                .thenReturn(PHOTO_ID);

        // 2. Exercise
        photoService.uploadCarousel(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);

    }

    @Test
    public void testUploadCarousel_MaxReached() {
        // 1. Setup
        Mockito.when(photoDao.countByClubIdAndType(Mockito.eq(CLUB_ID), Mockito.eq(PhotoType.CAROUSEL)))
                .thenReturn(20L);

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalStateException.class, () -> {
            photoService.uploadCarousel(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);
        });
    }

    // ========================
    // LIST BY CLUB
    // ========================

    @Test
    public void testListByClub() {
        // 1. Setup
        List<Photo> photos = Arrays.asList(mockPhoto);
        Mockito.when(photoDao.findByClub(Mockito.eq(CLUB_ID), Mockito.eq(10), Mockito.eq(0)))
                .thenReturn(photos);

        // 2. Exercise
        List<Photo> result = photoService.listByClub(CLUB_ID, 0, 10);

        // 3. Assert
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockPhoto));
    }

    // ========================
    // FIND BY ID
    // ========================

    @Test
    public void testFindById_Exists() {
        // 1. Setup
        Mockito.when(photoDao.findById(Mockito.eq(PHOTO_ID)))
                .thenReturn(Optional.of(mockPhoto));

        // 2. Exercise
        Optional<Photo> result = photoService.findById(PHOTO_ID);

        // 3. Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(mockPhoto, result.get());
    }

    @Test
    public void testFindById_NotExists() {
        // 1. Setup
        Mockito.when(photoDao.findById(Mockito.eq(PHOTO_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise
        Optional<Photo> result = photoService.findById(PHOTO_ID);

        // 3. Assert
        Assert.assertFalse(result.isPresent());
    }

    // ========================
    // GET WITH ID
    // ========================

    @Test
    public void testGetWithId_Exists() {
        // 1. Setup
        Mockito.when(photoDao.findByIdWithData(Mockito.eq(PHOTO_ID)))
                .thenReturn(Optional.of(mockPhoto));

        // 2. Exercise
        Optional<Photo> result = photoService.getWithId(PHOTO_ID);

        // 3. Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertArrayEquals(DATA, result.get().getData());
    }

    // ========================
    // DELETE
    // ========================

    @Test
    public void testDelete_Success() {
        // 1. Setup
        Mockito.when(photoDao.delete(Mockito.eq(PHOTO_ID)))
                .thenReturn(true);

        // 2. Exercise
        boolean result = photoService.delete(PHOTO_ID);

        // 3. Assert
        Assert.assertTrue(result);
    }

    @Test
    public void testDelete_Fail() {
        // 1. Setup
        Mockito.when(photoDao.delete(Mockito.eq(PHOTO_ID)))
                .thenReturn(false);

        // 2. Exercise
        boolean result = photoService.delete(PHOTO_ID);

        // 3. Assert
        Assert.assertFalse(result);
    }

    // ========================
    // UPLOAD PHOTO FOR CLUB
    // ========================

    @Test
    public void testUploadPhotoForClub_FileNull() {

        // 1. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadPhotoForClub(CLUB_ID, "user@club.com", null);
        });
    }

    @Test
    public void testUploadPhotoForClub_UserNotFound() {
        // 1. Setup
        Photo file = new Photo();
        file.setFilename(FILENAME);
        file.setContentType(CONTENT_TYPE);
        file.setData(DATA);

        Mockito.when(userService.findByEmail(Mockito.anyString()))
                .thenReturn(null);

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadPhotoForClub(CLUB_ID, "unknown@club.com", file);
        });
    }

    // ========================
    // GET COVER URL - HAPPY PATH
    // ========================

    @Test
    public void testGetCoverUrl_CoverExists_NotInEvent() {
        // 1. Setup
        mockPhoto.setPhotoType(PhotoType.COVER);
        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockPhoto));
        Mockito.when(eventDao.findByPhotoIdAndClub(Mockito.eq(PHOTO_ID), Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise
        Optional<String> result = photoService.getCoverUrl(CLUB_ID);

        // 3. Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(URL, result.get());
    }

    @Test
    public void testGetCoverUrl_CoverInEvent_FallbackCarousel() {
        // 1. Setup
        Photo cover = new Photo();
        cover.setPhotoId(PHOTO_ID);
        cover.setPhotoType(PhotoType.COVER);

        Photo carousel = new Photo();
        carousel.setPhotoId(UUID.randomUUID());
        carousel.setUrl("/api/photos/carousel1");
        carousel.setCreatedAt(OffsetDateTime.now().minusDays(1)); // Más antigua

        Photo carousel2 = new Photo();
        carousel2.setPhotoId(UUID.randomUUID());
        carousel2.setUrl("/api/photos/carousel2");
        carousel2.setCreatedAt(OffsetDateTime.now()); // Más reciente → debe ganar

        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(cover));
        Mockito.when(eventDao.findByPhotoIdAndClub(Mockito.eq(PHOTO_ID), Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(new Event()));
        Mockito.when(photoDao.findCarouselWithoutEventByClubId(Mockito.eq(CLUB_ID), Mockito.eq(50), Mockito.eq(0)))
                .thenReturn(Arrays.asList(carousel, carousel2));

        // 2. Exercise
        Optional<String> result = photoService.getCoverUrl(CLUB_ID);

        // 3. Assert
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("/api/photos/carousel2", result.get()); // La más reciente
    }

    @Test
    public void testGetCoverUrl_NoCover_NoCarousel() {
        // 1. Setup
        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());
        Mockito.when(photoDao.findCarouselWithoutEventByClubId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(Collections.emptyList());

        // 2. Exercise
        Optional<String> result = photoService.getCoverUrl(CLUB_ID);

        // 3. Assert
        Assert.assertFalse(result.isPresent());
    }

    // ========================
    // GET COVER URLS
    // ========================

    @Test
    public void testGetCoverUrls() {
        // 1. Setup
        Set<UUID> clubIds = Set.of(CLUB_ID);
        Map<UUID, String> expected = Map.of(CLUB_ID, URL);

        // CORREGIDO: Quitar "busters"
        Mockito.when(photoDao.findAllCoversByClubIds(Mockito.eq(clubIds)))
                .thenReturn(expected);

        // 2. Exercise
        Map<UUID, String> result = photoService.getCoverUrls(clubIds);

        // 3. Assert
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(URL, result.get(CLUB_ID));
    }

    // ========================
    // GET CAROUSEL PHOTOS
    // ========================

    @Test
    public void testGetCarouselPhotos() {
        // 1. Setup
        List<Photo> photos = Arrays.asList(mockPhoto);
        Mockito.when(photoDao.findCarouselByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(photos);

        // 2. Exercise
        List<Photo> result = photoService.getCarouselPhotos(CLUB_ID);

        // 3. Assert
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockPhoto));
    }

    // ========================
    // GET CLUB PHOTOS
    // ========================

    @Test
    public void testGetClubPhotos() {
        // 1. Setup
        Photo cover = new Photo();
        cover.setPhotoType(PhotoType.COVER);
        List<Photo> carousel = Arrays.asList(mockPhoto);

        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(cover));
        Mockito.when(photoDao.findCarouselByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(carousel);

        // 2. Exercise
        List<Photo> result = photoService.getClubPhotos(CLUB_ID, 10, 0);

        // 3. Assert
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(cover));
        Assert.assertTrue(result.contains(mockPhoto));
    }

    // ========================
    // UPLOAD REVIEW PHOTO
    // ========================

    @Test
    public void testUploadReviewPhoto_Success() {
        // 1. Setup
        Photo file = new Photo();
        file.setFilename(FILENAME);
        file.setContentType(CONTENT_TYPE);
        file.setData(DATA);

        Mockito.when(photoDao.createReviewPhoto(
                Mockito.eq(CLUB_ID), Mockito.eq(REVIEW_ID), Mockito.eq(USER_ID),
                Mockito.eq(FILENAME), Mockito.eq(CONTENT_TYPE), Mockito.eq(DATA)
        )).thenReturn(PHOTO_ID);

        // 2. Exercise
        UUID result = photoService.uploadReviewPhoto(CLUB_ID, REVIEW_ID, USER_ID, file);

        // 3. Assert
        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test
    public void testUploadReviewPhoto_FileNull() {

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadReviewPhoto(CLUB_ID, REVIEW_ID, USER_ID, null);
        });
    }

    // ========================
    // UPLOAD EVENT PHOTO
    // ========================

    @Test
    public void testUploadEventPhoto_Success() {
        // 1. Setup
        Photo file = new Photo();
        file.setFilename(FILENAME);
        file.setContentType(CONTENT_TYPE);
        file.setData(DATA);

        Mockito.when(photoDao.createEventPhoto(
                Mockito.eq(CLUB_ID), Mockito.eq(EVENT_ID), Mockito.eq(USER_ID),
                Mockito.eq(FILENAME), Mockito.eq(CONTENT_TYPE), Mockito.eq(DATA)
        )).thenReturn(PHOTO_ID);

        // 2. Exercise
        UUID result = photoService.uploadEventPhoto(CLUB_ID, EVENT_ID, USER_ID, file);

        // 3. Assert
        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test
    public void testUploadEventPhoto_ClubIdNull() {
        // 1. Setup
        // (ningún mock necesario)

        // 2. Exercise + 3. Assert
        Assert.assertThrows(NullPointerException.class, () -> {
            photoService.uploadEventPhoto(null, EVENT_ID, USER_ID, mockPhoto);
        });
    }

    @Test
    public void testUploadEventPhoto_FileNull() {
        // 1. Setup
        // (ningún mock necesario)

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadEventPhoto(CLUB_ID, EVENT_ID, USER_ID, null);
        });
    }

    // ========================
    // GET REVIEW PHOTOS BY CLUB
    // ========================

    @Test
    public void testGetReviewPhotosByClub() {
        // 1. Setup
        List<Photo> photos = Arrays.asList(mockPhoto);
        Mockito.when(photoDao.findReviewPhotosByClub(Mockito.eq(CLUB_ID), Mockito.eq(10), Mockito.eq(0)))
                .thenReturn(photos);

        // 2. Exercise
        List<Photo> result = photoService.getReviewPhotosByClub(CLUB_ID, 10, 0);

        // 3. Assert
        Assert.assertEquals(1, result.size());
    }
}