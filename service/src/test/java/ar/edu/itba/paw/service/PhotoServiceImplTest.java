package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;
import ar.edu.itba.paw.persistence.PhotoDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class PhotoServiceImplTest {
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID POSTED_BY = UUID.randomUUID();
    private static final String FILENAME = "test.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final byte[] DATA = new byte[]{1, 2, 3};
    private static final PhotoType PHOTO_TYPE = PhotoType.CAROUSEL;

    @Mock
    private PhotoDao photoDao;

    @InjectMocks
    private PhotoServiceImpl photoService;

    @Test
    public void testUploadSuccess() {
        Mockito.when(photoDao.create(CLUB_ID, POSTED_BY, FILENAME, CONTENT_TYPE, DATA, PHOTO_TYPE)).thenReturn(PHOTO_ID);
        UUID result = photoService.upload(CLUB_ID, POSTED_BY, FILENAME, CONTENT_TYPE, DATA);
        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadEmptyData() {
        photoService.upload(CLUB_ID, POSTED_BY, FILENAME, CONTENT_TYPE, new byte[]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadNullData() {
        photoService.upload(CLUB_ID, POSTED_BY, FILENAME, CONTENT_TYPE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadInvalidContentType() {
        photoService.upload(CLUB_ID, POSTED_BY, FILENAME, "image/gif", DATA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadInvalidFilename() {
        photoService.upload(CLUB_ID, POSTED_BY, " ", CONTENT_TYPE, DATA);
    }

    @Test
    public void testListByClub() {
        Photo photo1 = Mockito.mock(Photo.class);
        Photo photo2 = Mockito.mock(Photo.class);
        Mockito.when(photoDao.findByClub(CLUB_ID, 2, 0)).thenReturn(Arrays.asList(photo1, photo2));
        List<Photo> result = photoService.listByClub(CLUB_ID, 0, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testFindByBooking() {
        Photo photo = Mockito.mock(Photo.class);
        Mockito.when(photoDao.findByBooking(BOOKING_ID)).thenReturn(Optional.of(photo));
        Optional<Photo> result = photoService.findByBooking(BOOKING_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(photo, result.get());
    }

    @Test
    public void testFindById() {
        Photo photo = Mockito.mock(Photo.class);
        Mockito.when(photoDao.findById(PHOTO_ID)).thenReturn(Optional.of(photo));
        Optional<Photo> result = photoService.findById(PHOTO_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(photo, result.get());
    }

    @Test
    public void testGetWithId() {
        Photo photo = Mockito.mock(Photo.class);
        Mockito.when(photoDao.findByIdWithData(PHOTO_ID)).thenReturn(Optional.of(photo));
        Optional<Photo> result = photoService.getWithId(PHOTO_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(photo, result.get());
    }

    @Test
    public void testSave() {
        Photo photo = Mockito.mock(Photo.class);
        Mockito.when(photoDao.save(photo)).thenReturn(PHOTO_ID);
        UUID result = photoService.save(photo);
        Assert.assertEquals(PHOTO_ID, result);
    }

    @Test
    public void testDelete() {
        Mockito.when(photoDao.delete(PHOTO_ID)).thenReturn(true);
        boolean result = photoService.delete(PHOTO_ID);
        Assert.assertTrue(result);
    }
}
