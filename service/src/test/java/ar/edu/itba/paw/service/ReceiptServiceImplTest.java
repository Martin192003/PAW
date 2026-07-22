package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.BookingNotFoundException;
import ar.edu.itba.paw.model.exceptions.InvalidReceiptException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.persistence.ReceiptDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
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

    @Mock private ReceiptDao receiptDao;
    @Mock private BookingService bookingService;
    @Mock private TableService tableService;
    @Mock private ClubService clubService;
    @Mock private EmailService emailService;

    @InjectMocks private ReceiptServiceImpl receiptService;

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

    @Test
    public void testUploadForBookingSuccess() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getStatus()).thenReturn(Status.PENDIENTE);

        Mockito.when(receiptDao.findByBooking(BOOKING_ID)).thenReturn(Optional.empty());
        Object[] createArgs = new Object[5];
        Mockito.when(receiptDao.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    for (int i = 0; i < 5; i++) {
                        createArgs[i] = invocation.getArgument(i);
                    }
                    return RECEIPT_ID;
                });

        ClubTable table = Mockito.mock(ClubTable.class);
        Club club = Mockito.mock(Club.class);
        User admin = Mockito.mock(User.class);

        Mockito.when(booking.getTableId()).thenReturn(UUID.randomUUID());
        Mockito.when(table.getTableId()).thenReturn(UUID.randomUUID());
        Mockito.when(table.getClubId()).thenReturn(UUID.randomUUID());
        Mockito.when(club.getClubId()).thenReturn(UUID.randomUUID());
        Mockito.when(club.getClubName()).thenReturn("Club Test");
        Mockito.when(admin.getEmail()).thenReturn("admin@club.com");

        Mockito.when(tableService.findById(Mockito.any())).thenReturn(Optional.of(table));
        Mockito.when(clubService.getClubById(Mockito.any())).thenReturn(Optional.of(club));
        Mockito.when(clubService.getAdminsByClub(Mockito.any())).thenReturn(List.of(admin));

        receiptService.uploadForBooking(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, DATA);

        Assert.assertEquals(
                List.of(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE),
                List.of(createArgs[0], createArgs[1], createArgs[2], createArgs[3]));
        Assert.assertArrayEquals(DATA, (byte[]) createArgs[4]);
    }

    @Test
    public void testUploadForBookingNormalizesJpgAlias() {
        mockValidBookingForUpload();
        Mockito.when(receiptDao.findByBooking(BOOKING_ID)).thenReturn(Optional.empty());
        String[] capturedContentType = new String[1];
        Mockito.when(receiptDao.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    capturedContentType[0] = invocation.getArgument(3);
                    return RECEIPT_ID;
                });

        receiptService.uploadForBooking(BOOKING_ID, UPLOADED_BY, "recibo.jpg", "image/jpg", DATA);

        Assert.assertEquals("image/jpeg", capturedContentType[0]);
    }

    @Test
    public void testUploadForBookingInfersOctetStreamFromFilename() {
        mockValidBookingForUpload();
        Mockito.when(receiptDao.findByBooking(BOOKING_ID)).thenReturn(Optional.empty());
        String[] capturedContentType = new String[1];
        Mockito.when(receiptDao.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    capturedContentType[0] = invocation.getArgument(3);
                    return RECEIPT_ID;
                });

        receiptService.uploadForBooking(BOOKING_ID, UPLOADED_BY, "recibo.png", "application/octet-stream", DATA);

        Assert.assertEquals("image/png", capturedContentType[0]);
    }

    @Test
    public void testDeleteByBookingForUserSuccess() {
        Booking booking = Mockito.mock(Booking.class);
        Receipt receipt = Mockito.mock(Receipt.class);

        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getStatus()).thenReturn(Status.PENDIENTE);
        Mockito.when(receiptDao.findByBooking(BOOKING_ID)).thenReturn(Optional.of(receipt));
        Mockito.when(receipt.getReceiptId()).thenReturn(RECEIPT_ID);
        Mockito.when(receiptDao.delete(RECEIPT_ID)).thenReturn(true);

        boolean result = receiptService.deleteByBookingForUser(BOOKING_ID, UPLOADED_BY);

        Assert.assertTrue(result);
    }

    @Test
    public void testUploadForBookingConfirmedThrows() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getStatus()).thenReturn(Status.CONFIRMADA);

        Assert.assertThrows(InvalidReceiptException.class, () ->
                receiptService.uploadForBooking(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, DATA));
    }

    @Test
    public void testUploadForBookingCancelledThrows() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getStatus()).thenReturn(Status.CANCELADA);


        Assert.assertThrows(InvalidReceiptException.class, () ->
                receiptService.uploadForBooking(BOOKING_ID, UPLOADED_BY, FILENAME, CONTENT_TYPE, DATA));
    }

    @Test
    public void testDeleteByBookingForUserConfirmedThrows() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getStatus()).thenReturn(Status.CONFIRMADA);


        Assert.assertThrows(InvalidReceiptException.class, () ->
                receiptService.deleteByBookingForUser(BOOKING_ID, UPLOADED_BY));
    }

    @Test
    public void testPutForBookingInClubSuccess() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(booking.getStatus()).thenReturn(Status.PENDIENTE);
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getTableId()).thenReturn(UUID.randomUUID());

        Mockito.when(bookingService.getForUserOrAdminInClub(Mockito.any(), Mockito.eq(BOOKING_ID), Mockito.any()))
                .thenReturn(booking);
        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        Receipt newReceipt = Mockito.mock(Receipt.class);
        Mockito.when(newReceipt.getReceiptId()).thenReturn(RECEIPT_ID);

        Mockito.when(receiptDao.findByBooking(BOOKING_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(newReceipt));

        Mockito.when(receiptDao.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(RECEIPT_ID);
        Mockito.when(receiptDao.findByIdWithData(RECEIPT_ID)).thenReturn(Optional.of(newReceipt));
        Mockito.when(tableService.findById(Mockito.any())).thenReturn(Optional.empty());

        UUID someClubId = UUID.randomUUID();
        Receipt result = receiptService.putForBookingInClub(someClubId, BOOKING_ID, "admin@club.com", FILENAME, CONTENT_TYPE, DATA);

        Assert.assertEquals(newReceipt, result);
    }

    @Test
    public void testPutForBookingInClubConfirmedThrows() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(booking.getStatus()).thenReturn(Status.CONFIRMADA);
        Mockito.when(bookingService.getForUserOrAdminInClub(Mockito.any(), Mockito.eq(BOOKING_ID), Mockito.any()))
                .thenReturn(booking);

        UUID someClubId = UUID.randomUUID();
        Assert.assertThrows(InvalidReceiptException.class, () ->
                receiptService.putForBookingInClub(someClubId, BOOKING_ID, "admin@club.com", FILENAME, CONTENT_TYPE, DATA));
    }

    @Test
    public void testGetReceiptForBooking() {
        Receipt meta = Mockito.mock(Receipt.class);
        Receipt full = Mockito.mock(Receipt.class);

        Mockito.when(receiptDao.findByBooking(BOOKING_ID)).thenReturn(Optional.of(meta));
        Mockito.when(meta.getReceiptId()).thenReturn(RECEIPT_ID);
        Mockito.when(receiptDao.findByIdWithData(RECEIPT_ID)).thenReturn(Optional.of(full));

        Optional<Receipt> res = receiptService.getReceiptForBooking(BOOKING_ID);

        Assert.assertTrue(res.isPresent());
        Assert.assertEquals(full, res.get());
    }

    private void mockValidBookingForUpload() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingService.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getUser()).thenReturn(new User(UPLOADED_BY));
        Mockito.when(booking.getStatus()).thenReturn(Status.PENDIENTE);
        Mockito.when(booking.getTableId()).thenReturn(UUID.randomUUID());
        Mockito.when(tableService.findById(Mockito.any())).thenReturn(Optional.empty());
    }
}
