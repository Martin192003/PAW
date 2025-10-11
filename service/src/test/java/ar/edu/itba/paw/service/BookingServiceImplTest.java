package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
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
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class BookingServiceImplTest {
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final OffsetDateTime START = OffsetDateTime.now();
    private static final OffsetDateTime END = START.plusHours(2);
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);

    @Mock
    private BookingDao bookingDao;
    @Mock
    private ClubTablesDao tablesDao;
    @Mock
    private ClubDao clubDao;
    @Mock
    private EmailServiceImpl emailService;
    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    public void testFindByIdFound() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Optional<Booking> result = bookingService.findById(BOOKING_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(booking, result.get());
    }

    @Test
    public void testFindByIdNotFound() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.empty());
        Optional<Booking> result = bookingService.findById(BOOKING_ID);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindByUser() {
        Booking booking1 = Mockito.mock(Booking.class);
        Booking booking2 = Mockito.mock(Booking.class);
        Mockito.when(bookingDao.findByUser(USER_ID)).thenReturn(Arrays.asList(booking1, booking2));
        List<Booking> bookings = bookingService.findByUser(USER_ID);
        Assert.assertNotNull(bookings);
        Assert.assertEquals(2, bookings.size());
    }

    @Test
    public void testCreateBookingSuccess() {
        User user = Mockito.mock(User.class);
        ClubTable table = Mockito.mock(ClubTable.class);
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.of(table));
        Mockito.when(table.getTableState()).thenReturn(TableState.DISPONIBLE);
        Mockito.when(bookingDao.create(user, TABLE_ID, Status.PENDIENTE, AMOUNT, START, END)).thenReturn(booking);
        Booking result = bookingService.create(user, TABLE_ID, START, END, AMOUNT);
        Assert.assertNotNull(result);
        Assert.assertEquals(booking, result);
    }

    @Test(expected = TableAlreadyBookedException.class)
    public void testCreateBookingTableNotAvailable() {
        User user = Mockito.mock(User.class);
        ClubTable table = Mockito.mock(ClubTable.class);
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.of(table));
        Mockito.when(table.getTableState()).thenReturn(TableState.RESERVADA);
        bookingService.create(user, TABLE_ID, START, END, AMOUNT);
    }

    @Test(expected = TableNotFoundException.class)
    public void testCreateBookingTableNotFound() {
        User user = Mockito.mock(User.class);
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.empty());
        bookingService.create(user, TABLE_ID, START, END, AMOUNT);
    }

    @Test
    public void testUpdateStatusSuccess() {
        Booking booking = Mockito.mock(Booking.class);
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getStatus()).thenReturn(Status.PENDIENTE);
        Mockito.when(booking.getUser()).thenReturn(Mockito.mock(User.class));
        Mockito.when(booking.getTableId()).thenReturn(TABLE_ID);
        Mockito.when(booking.getTotalAmount()).thenReturn(AMOUNT);
        Mockito.when(booking.getBookingStart()).thenReturn(START);
        Mockito.when(booking.getBookingEnd()).thenReturn(END);
        Booking updated = Mockito.mock(Booking.class);
        Mockito.when(bookingDao.update(BOOKING_ID, booking.getUser(), TABLE_ID, Status.CONFIRMADA, AMOUNT, START, END)).thenReturn(Optional.ofNullable(updated));
        Booking result = bookingService.updateStatus(BOOKING_ID, Status.CONFIRMADA);
        Assert.assertNotNull(result);
        Assert.assertEquals(updated, result);
    }

    @Test(expected = BookingNotFoundException.class)
    public void testUpdateStatusBookingNotFound() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.empty());
        bookingService.updateStatus(BOOKING_ID, Status.CONFIRMADA);
    }

    @Test
    public void testCancelBookingSuccess() {
        Booking booking = Mockito.mock(Booking.class);
        ClubTable table = Mockito.mock(ClubTable.class);
        Club club = Mockito.mock(Club.class);
        User user = Mockito.mock(User.class);
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(booking.getStatus()).thenReturn(Status.PENDIENTE);
        Mockito.when(booking.getUser()).thenReturn(user);
        Mockito.when(booking.getTableId()).thenReturn(TABLE_ID);
        Mockito.when(booking.getTotalAmount()).thenReturn(AMOUNT);
        Mockito.when(booking.getBookingStart()).thenReturn(START);
        Mockito.when(booking.getBookingEnd()).thenReturn(END);
        Mockito.when(bookingDao.update(BOOKING_ID, user, TABLE_ID, Status.CANCELADA, AMOUNT, START, END)).thenReturn(Optional.of(booking));
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.of(table));
        Mockito.when(table.getClubId()).thenReturn(CLUB_ID);
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(user.getId()).thenReturn(USER_ID);
        Mockito.when(userService.findById(USER_ID)).thenReturn(user);
        bookingService.cancel(BOOKING_ID);


    }

    @Test(expected = BookingNotFoundException.class)
    public void testCancelBookingNotFound() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.empty());
        bookingService.cancel(BOOKING_ID);
    }
}
