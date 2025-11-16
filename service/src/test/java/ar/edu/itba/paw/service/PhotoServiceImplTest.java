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

    @Test
    public void testUpload_Success() {
        Mockito.when(photoDao.save(Mockito.any(Photo.class)))
                .thenReturn(PHOTO_ID);

        UUID result = photoService.upload(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);

        Assert.assertNotNull(result);
        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test
    public void testUploadCover_Success() {
        Mockito.doNothing().when(photoDao).deleteCoverByClubId(Mockito.eq(CLUB_ID));
        Mockito.when(photoDao.save(Mockito.any(Photo.class)))
                .thenReturn(PHOTO_ID);

        photoService.uploadCover(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);
    }

    @Test
    public void testUploadCarousel_Success() {
        Mockito.when(photoDao.countByClubIdAndType(Mockito.eq(CLUB_ID), Mockito.eq(PhotoType.CAROUSEL)))
                .thenReturn(19L);
        Mockito.when(photoDao.save(Mockito.any(Photo.class)))
                .thenReturn(PHOTO_ID);

        photoService.uploadCarousel(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);

    }

    @Test
    public void testUploadCarousel_MaxReached() {
        Mockito.when(photoDao.countByClubIdAndType(Mockito.eq(CLUB_ID), Mockito.eq(PhotoType.CAROUSEL)))
                .thenReturn(20L);

        Assert.assertThrows(IllegalStateException.class, () -> {
            photoService.uploadCarousel(CLUB_ID, USER_ID, FILENAME, CONTENT_TYPE, DATA);
        });
    }

    @Test
    public void testListByClub() {
        List<Photo> photos = Arrays.asList(mockPhoto);
        Mockito.when(photoDao.findByClub(Mockito.eq(CLUB_ID), Mockito.eq(10), Mockito.eq(0)))
                .thenReturn(photos);

        List<Photo> result = photoService.listByClub(CLUB_ID, 0, 10);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockPhoto));
    }

    @Test
    public void testFindById_Exists() {
        Mockito.when(photoDao.findById(Mockito.eq(PHOTO_ID)))
                .thenReturn(Optional.of(mockPhoto));

        Optional<Photo> result = photoService.findById(PHOTO_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(mockPhoto, result.get());
    }

    @Test
    public void testFindById_NotExists() {
        Mockito.when(photoDao.findById(Mockito.eq(PHOTO_ID)))
                .thenReturn(Optional.empty());

        Optional<Photo> result = photoService.findById(PHOTO_ID);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testGetWithId_Exists() {
        Mockito.when(photoDao.findByIdWithData(Mockito.eq(PHOTO_ID)))
                .thenReturn(Optional.of(mockPhoto));

        Optional<Photo> result = photoService.getWithId(PHOTO_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertArrayEquals(DATA, result.get().getData());
    }

    @Test
    public void testDelete_Success() {
        Mockito.when(photoDao.delete(Mockito.eq(PHOTO_ID)))
                .thenReturn(true);

        boolean result = photoService.delete(PHOTO_ID);

        Assert.assertTrue(result);
    }

    @Test
    public void testDelete_Fail() {
        Mockito.when(photoDao.delete(Mockito.eq(PHOTO_ID)))
                .thenReturn(false);

        boolean result = photoService.delete(PHOTO_ID);

        Assert.assertFalse(result);
    }

    @Test
    public void testUploadPhotoForClub_FileNull() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadPhotoForClub(CLUB_ID, "user@club.com", null);
        });
    }

    @Test
    public void testUploadPhotoForClub_UserNotFound() {
        Photo file = new Photo();
        file.setFilename(FILENAME);
        file.setContentType(CONTENT_TYPE);
        file.setData(DATA);

        Mockito.when(userService.findByEmail(Mockito.anyString()))
                .thenReturn(null);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadPhotoForClub(CLUB_ID, "unknown@club.com", file);
        });
    }

    @Test
    public void testGetCoverUrl_CoverExists_NotInEvent() {
        mockPhoto.setPhotoType(PhotoType.COVER);
        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockPhoto));
        Mockito.when(eventDao.findByPhotoIdAndClub(Mockito.eq(PHOTO_ID), Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());

        Optional<String> result = photoService.getCoverUrl(CLUB_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(URL, result.get());
    }

    @Test
    public void testGetCoverUrl_CoverInEvent_FallbackCarousel() {
        Photo cover = new Photo();
        cover.setPhotoId(PHOTO_ID);
        cover.setPhotoType(PhotoType.COVER);

        Photo carousel = new Photo();
        carousel.setPhotoId(UUID.randomUUID());
        carousel.setUrl("/api/photos/carousel1");
        carousel.setCreatedAt(OffsetDateTime.now().minusDays(1));

        Photo carousel2 = new Photo();
        carousel2.setPhotoId(UUID.randomUUID());
        carousel2.setUrl("/api/photos/carousel2");
        carousel2.setCreatedAt(OffsetDateTime.now());

        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(cover));
        Mockito.when(eventDao.findByPhotoIdAndClub(Mockito.eq(PHOTO_ID), Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(new Event()));
        Mockito.when(photoDao.findCarouselWithoutEventByClubId(Mockito.eq(CLUB_ID), Mockito.eq(50), Mockito.eq(0)))
                .thenReturn(Arrays.asList(carousel, carousel2));

        Optional<String> result = photoService.getCoverUrl(CLUB_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals("/api/photos/carousel2", result.get()); // La más reciente
    }

    @Test
    public void testGetCoverUrl_NoCover_NoCarousel() {
        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());
        Mockito.when(photoDao.findCarouselWithoutEventByClubId(Mockito.any(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(Collections.emptyList());

        Optional<String> result = photoService.getCoverUrl(CLUB_ID);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testGetCoverUrls() {
        Set<UUID> clubIds = Set.of(CLUB_ID);
        Map<UUID, String> expected = Map.of(CLUB_ID, URL);

        Mockito.when(photoDao.findAllCoversByClubIds(Mockito.eq(clubIds)))
                .thenReturn(expected);

        Map<UUID, String> result = photoService.getCoverUrls(clubIds);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(URL, result.get(CLUB_ID));
    }

    @Test
    public void testGetCarouselPhotos() {
        List<Photo> photos = Arrays.asList(mockPhoto);
        Mockito.when(photoDao.findCarouselByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(photos);

        List<Photo> result = photoService.getCarouselPhotos(CLUB_ID);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockPhoto));
    }

    @Test
    public void testGetClubPhotos() {
        Photo cover = new Photo();
        cover.setPhotoType(PhotoType.COVER);
        List<Photo> carousel = Arrays.asList(mockPhoto);

        Mockito.when(photoDao.findCoverByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(cover));
        Mockito.when(photoDao.findCarouselByClubId(Mockito.eq(CLUB_ID)))
                .thenReturn(carousel);

        List<Photo> result = photoService.getClubPhotos(CLUB_ID, 10, 0);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(cover));
        Assert.assertTrue(result.contains(mockPhoto));
    }

    @Test
    public void testUploadReviewPhoto_Success() {
        Photo file = new Photo();
        file.setFilename(FILENAME);
        file.setContentType(CONTENT_TYPE);
        file.setData(DATA);

        Mockito.when(photoDao.createReviewPhoto(
                Mockito.eq(CLUB_ID), Mockito.eq(REVIEW_ID), Mockito.eq(USER_ID),
                Mockito.eq(FILENAME), Mockito.eq(CONTENT_TYPE), Mockito.eq(DATA)
        )).thenReturn(PHOTO_ID);

        UUID result = photoService.uploadReviewPhoto(CLUB_ID, REVIEW_ID, USER_ID, file);

        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test
    public void testUploadReviewPhoto_FileNull() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadReviewPhoto(CLUB_ID, REVIEW_ID, USER_ID, null);
        });
    }

    @Test
    public void testUploadEventPhoto_Success() {
        Photo file = new Photo();
        file.setFilename(FILENAME);
        file.setContentType(CONTENT_TYPE);
        file.setData(DATA);

        Mockito.when(photoDao.createEventPhoto(
                Mockito.eq(CLUB_ID), Mockito.eq(EVENT_ID), Mockito.eq(USER_ID),
                Mockito.eq(FILENAME), Mockito.eq(CONTENT_TYPE), Mockito.eq(DATA)
        )).thenReturn(PHOTO_ID);

        UUID result = photoService.uploadEventPhoto(CLUB_ID, EVENT_ID, USER_ID, file);

        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test
    public void testUploadEventPhoto_ClubIdNull() {
        Assert.assertThrows(NullPointerException.class, () -> {
            photoService.uploadEventPhoto(null, EVENT_ID, USER_ID, mockPhoto);
        });
    }

    @Test
    public void testUploadEventPhoto_FileNull() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            photoService.uploadEventPhoto(CLUB_ID, EVENT_ID, USER_ID, null);
        });
    }

    @Test
    public void testGetReviewPhotosByClub() {
        List<Photo> photos = Arrays.asList(mockPhoto);
        Mockito.when(photoDao.findReviewPhotosByClub(Mockito.eq(CLUB_ID), Mockito.eq(10), Mockito.eq(0)))
                .thenReturn(photos);

        List<Photo> result = photoService.getReviewPhotosByClub(CLUB_ID, 10, 0);

        Assert.assertEquals(1, result.size());
    }
}