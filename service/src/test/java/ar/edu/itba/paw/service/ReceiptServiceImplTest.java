package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.persistence.ReceiptDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ReceiptServiceImplTest {
    private static final UUID RECEIPT_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID UPLOADED_BY = UUID.randomUUID();
    private static final String FILENAME = "recibo.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final byte[] DATA = new byte[]{1, 2, 3};

    @Mock
    private ReceiptDao receiptDao;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    @Test
    public void testUploadSuccess() {
        Mockito.when(receiptDao.create(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, DATA)).thenReturn(RECEIPT_ID);
        UUID result = receiptService.upload(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, DATA);
        Assert.assertEquals(RECEIPT_ID, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadEmptyData() {
        receiptService.upload(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, new byte[]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadNullData() {
        receiptService.upload(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadInvalidContentType() {
        receiptService.upload(BOOKING_ID, UPLOADED_BY, FILENAME, "image/gif", DATA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUploadInvalidFilename() {
        receiptService.upload(BOOKING_ID, UPLOADED_BY, " ", CONTENT_TYPE, DATA);
    }

    @Test
    public void testFindByBooking() {
        Receipt receipt = Mockito.mock(Receipt.class);
        Mockito.when(receiptDao.findByBooking(BOOKING_ID)).thenReturn(Optional.of(receipt));
        Optional<Receipt> result = receiptService.findByBooking(BOOKING_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(receipt, result.get());
    }

    @Test
    public void testGetWithId() {
        Receipt receipt = Mockito.mock(Receipt.class);
        Mockito.when(receiptDao.findByIdWithData(RECEIPT_ID)).thenReturn(Optional.of(receipt));
        Optional<Receipt> result = receiptService.getWithId(RECEIPT_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(receipt, result.get());
    }

    @Test
    public void testDeleteByBooking() {
        Mockito.when(receiptDao.deleteByBooking(BOOKING_ID)).thenReturn(true);
        boolean result = receiptService.deleteByBooking(BOOKING_ID);
        Assert.assertTrue(result);
    }
}
