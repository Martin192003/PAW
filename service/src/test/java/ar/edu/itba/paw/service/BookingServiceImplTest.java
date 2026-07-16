package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.persistence.ReceiptDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class BookingServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final BigDecimal TOTAL_AMOUNT = BigDecimal.TEN;

    private static final OffsetDateTime START = OffsetDateTime.now().plusDays(1).withHour(20).withMinute(0);
    private static final OffsetDateTime END = START.plusHours(4);

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock
    private BookingDao bookingDao;

    @Mock
    private ClubTablesDao tablesDao;

    @Mock
    private UserService userService;

    @Mock
    private ClubService clubService;

    @Mock
    private TableService tableService;

    @Mock
    private ClubDao clubDao;

    @Mock
    private EmailService emailService;

    @Mock
    private ReceiptDao receiptDao;

    private User mockUser;
    private ClubTable mockTable;
    private Booking mockBooking;

    @Before
    public void setUp() {
        mockUser = new User();
        mockUser.setUserId(USER_ID);

        mockTable = new ClubTable();
        mockTable.setTableId(TABLE_ID);
        mockTable.setClubId(CLUB_ID);
        mockTable.setTableState(TableState.DISPONIBLE);

        mockBooking = new Booking();
        mockBooking.setBookingId(BOOKING_ID);
        mockBooking.setUser(mockUser);
        mockBooking.setTableId(TABLE_ID);
        mockBooking.setTable(mockTable);
        mockBooking.setBookingStart(START);
        mockBooking.setBookingEnd(END);
        mockBooking.setTotalAmount(TOTAL_AMOUNT);
        mockBooking.setStatus(Status.PENDIENTE);
    }

    @Test
    public void testCreate_Success() {
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.of(mockTable));
        Mockito.when(bookingDao.findByTable(TABLE_ID)).thenReturn(List.of());
        Mockito.when(bookingDao.create(mockUser, TABLE_ID, Status.PENDIENTE, TOTAL_AMOUNT, START, END))
                .thenReturn(mockBooking);

        Booking result = bookingService.create(mockUser, TABLE_ID, START, END, TOTAL_AMOUNT);

        Assert.assertNotNull(result);
        Assert.assertEquals(BOOKING_ID, result.getBookingId());
        Assert.assertEquals(mockUser, result.getUser());
        Assert.assertEquals(TABLE_ID, result.getTableId());
        Assert.assertEquals(Status.PENDIENTE, result.getStatus());
        Assert.assertEquals(START, result.getBookingStart());
        Assert.assertEquals(END, result.getBookingEnd());
        Assert.assertEquals(TOTAL_AMOUNT, result.getTotalAmount());
    }

    @Test
    public void testCreate_NullUser() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            bookingService.create(null, TABLE_ID, START, END, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreate_TableNotAvailable() {
        mockTable.setTableState(TableState.RESERVADA);
        Mockito.when(tablesDao.findById(Mockito.eq(TABLE_ID)))
                .thenReturn(Optional.of(mockTable));

        Assert.assertThrows(TableAlreadyBookedException.class, () -> {
            bookingService.create(mockUser, TABLE_ID, START, END, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreate_OverlapWithActiveBooking() {
        Booking overlapping = new Booking();
        overlapping.setBookingId(UUID.randomUUID());
        overlapping.setBookingStart(START.minusHours(1));
        overlapping.setBookingEnd(END.minusHours(1));
        overlapping.setStatus(Status.PENDIENTE);

        Mockito.when(tablesDao.findById(Mockito.eq(TABLE_ID)))
                .thenReturn(Optional.of(mockTable));
        Mockito.when(bookingDao.findByTable(Mockito.eq(TABLE_ID)))
                .thenReturn(List.of(overlapping));

        Assert.assertThrows(TableAlreadyBookedException.class, () -> {
            bookingService.create(mockUser, TABLE_ID, START, END, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreate_InvalidDateRange_StartAfterEnd() {
        Assert.assertThrows(InvalidDateRangeException.class, () -> {
            bookingService.create(mockUser, TABLE_ID, END, START, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreateForClubWithoutOwnerDoesNotCreateBooking() {
        mockTable.setPrice(TOTAL_AMOUNT);
        mockUser.setEmail("user@example.com");

        Club club = new Club();
        club.setClubId(CLUB_ID);

        Mockito.when(tableService.findById(TABLE_ID)).thenReturn(Optional.of(mockTable));
        Mockito.when(tableService.findAvailableByClubAndRange(Mockito.eq(CLUB_ID), any(), any()))
                .thenReturn(List.of(mockTable));
        Mockito.when(userService.findByUserName("user@example.com")).thenReturn(mockUser);
        Mockito.when(clubService.getClubById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(clubService.getAdminsByClub(CLUB_ID)).thenReturn(List.of());

        // createForClub valida que el club tenga un admin ANTES de llegar a
        // bookingDao.create; si esa guarda fallara, no se lanzaría InvalidClubException y
        // este assertThrows lo detectaría solo, sin necesitar un verify(never()) aparte.
        Assert.assertThrows(InvalidClubException.class, () ->
                bookingService.createForClub(CLUB_ID, TABLE_ID, LocalDate.now().plusDays(1), "user@example.com")
        );
    }

    @Test
    public void testUpdate_Success() {
        OffsetDateTime newStart = START.plusHours(1);
        OffsetDateTime newEnd = END.plusHours(1);
        BigDecimal newAmount = BigDecimal.valueOf(20);

        Booking updatedBooking = new Booking();
        updatedBooking.setBookingId(BOOKING_ID);
        updatedBooking.setStatus(Status.CONFIRMADA);
        updatedBooking.setBookingStart(newStart);
        updatedBooking.setBookingEnd(newEnd);
        updatedBooking.setTotalAmount(newAmount);

        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        Mockito.when(bookingDao.findByTable(TABLE_ID)).thenReturn(List.of(mockBooking));
        Mockito.when(bookingDao.update(BOOKING_ID, mockUser, TABLE_ID, Status.CONFIRMADA, newAmount, newStart, newEnd))
                .thenReturn(Optional.of(updatedBooking));

        Booking result = bookingService.update(BOOKING_ID, mockUser, TABLE_ID, newStart, newEnd, newAmount, Status.CONFIRMADA);

        Assert.assertNotNull(result);
        Assert.assertEquals(Status.CONFIRMADA, result.getStatus());
        Assert.assertEquals(newStart, result.getBookingStart());
        Assert.assertEquals(newEnd, result.getBookingEnd());
        Assert.assertEquals(newAmount, result.getTotalAmount());
    }

    @Test
    public void testUpdateStatus_ToConfirmed() {
        mockBooking.setStatus(Status.PENDIENTE);

        User user = new User();
        user.setUserId(USER_ID);
        user.setLocale("es");
        mockBooking.setUser(user);

        Booking updatedBooking = new Booking();
        updatedBooking.setBookingId(BOOKING_ID);
        updatedBooking.setStatus(Status.CONFIRMADA);
        updatedBooking.setUser(user);
        updatedBooking.setTable(mockBooking.getTable());

        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));
        Mockito.when(bookingDao.update(
                Mockito.eq(BOOKING_ID),
                Mockito.eq(user),
                Mockito.eq(TABLE_ID),
                Mockito.eq(Status.CONFIRMADA),
                Mockito.eq(mockBooking.getTotalAmount()),
                Mockito.eq(mockBooking.getBookingStart()),
                Mockito.eq(mockBooking.getBookingEnd())
        )).thenReturn(Optional.of(updatedBooking));

        Booking result = bookingService.updateStatus(BOOKING_ID, Status.CONFIRMADA);

        Assert.assertEquals(Status.CONFIRMADA, result.getStatus());
    }

    @Test
    public void testUpdateStatus_InvalidTransition() {
        mockBooking.setStatus(Status.CANCELADA);
        Mockito.when(bookingDao.findById(Mockito.eq(BOOKING_ID)))
                .thenReturn(Optional.of(mockBooking));

        Assert.assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.updateStatus(BOOKING_ID, Status.PENDIENTE);
        });
    }

    @Test
    public void testCancel_Success() {
        mockBooking.setStatus(Status.PENDIENTE);

        Club club = new Club();
        club.setClubId(CLUB_ID);

        User completeUser = new User();
        completeUser.setUserId(USER_ID);
        completeUser.setEmail("user@example.com");

        Mockito.when(bookingDao.findById(BOOKING_ID))
                .thenReturn(Optional.of(mockBooking));

        Mockito.when(tablesDao.findById(TABLE_ID))
                .thenReturn(Optional.of(mockTable));

        Mockito.when(clubDao.findById(CLUB_ID))
                .thenReturn(Optional.of(club));

        Mockito.when(userService.findById(USER_ID))
                .thenReturn(Optional.of(completeUser));

        Mockito.when(bookingDao.update(
                any(), any(), any(), any(), any(), any(), any()
        )).thenAnswer(inv -> {
            Status newStatus = inv.getArgument(3);
            mockBooking.setStatus(newStatus);
            return Optional.of(mockBooking);
        });

        bookingService.cancel(BOOKING_ID);

        Assert.assertEquals(Status.CANCELADA, mockBooking.getStatus());
    }

    @Test
    public void testConfirm_PendingToConfirmed() {
        mockBooking.setStatus(Status.PENDIENTE);
        mockBooking.setTableId(TABLE_ID);
        mockBooking.setUser(mockUser);

        Mockito.when(bookingDao.findById(BOOKING_ID))
                .thenReturn(Optional.of(mockBooking))
                .thenReturn(Optional.of(mockBooking));

        Mockito.when(receiptDao.findByBooking(BOOKING_ID))
                .thenReturn(Optional.of(Mockito.mock(Receipt.class)));

        Mockito.when(bookingDao.updateStatus(BOOKING_ID, Status.CONFIRMADA))
                .thenAnswer(inv -> {
                    mockBooking.setStatus(Status.CONFIRMADA);
                    return Optional.of(mockBooking);
                });

        Mockito.when(tablesDao.findById(TABLE_ID))
                .thenReturn(Optional.of(mockTable));

        Club club = new Club();
        club.setClubId(CLUB_ID);
        Mockito.when(clubDao.findById(CLUB_ID))
                .thenReturn(Optional.of(club));

        mockUser.setEmail("mail@test.com");
        mockUser.setLocale("es");
        Mockito.when(userService.findById(USER_ID))
                .thenReturn(Optional.of(mockUser));

        Mockito.doNothing().when(emailService)
                .notifyConfirmationEmailToUser(any(), any(), any(), any());

        bookingService.confirm(BOOKING_ID);

        Assert.assertEquals(Status.CONFIRMADA, mockBooking.getStatus());
    }

    @Test
    public void testConfirm_NoReceipt_ThrowsInvalidReceiptException() {
        mockBooking.setStatus(Status.PENDIENTE);

        Mockito.when(bookingDao.findById(BOOKING_ID))
                .thenReturn(Optional.of(mockBooking));
        Mockito.when(receiptDao.findByBooking(BOOKING_ID))
                .thenReturn(Optional.empty());

        // confirm() valida que exista un receipt ANTES de llegar a
        // bookingDao.updateStatus; si esa guarda fallara, no se lanzaría
        // InvalidReceiptException y este assertThrows lo detectaría solo, sin necesitar
        // un verify(never()) aparte.
        Assert.assertThrows(InvalidReceiptException.class, () -> {
            bookingService.confirm(BOOKING_ID);
        });
    }


    @Test
    public void testConfirm_AlreadyCancelled() {
        mockBooking.setStatus(Status.CANCELADA);
        Mockito.when(bookingDao.findById(Mockito.eq(BOOKING_ID)))
                .thenReturn(Optional.of(mockBooking));

        Assert.assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.confirm(BOOKING_ID);
        });
    }

    @Test
    public void testFindById_Exists() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        Optional<Booking> result = bookingService.findById(BOOKING_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(BOOKING_ID, result.get().getBookingId());
    }

    @Test
    public void testFindById_NotExists() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.empty());

        Optional<Booking> result = bookingService.findById(BOOKING_ID);

        Assert.assertFalse(result.isPresent());
    }


    @Test
    public void testGetForUser_Authorized() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        Booking result = bookingService.getForUser(BOOKING_ID, USER_ID);

        Assert.assertEquals(mockBooking, result);
    }

    @Test
    public void testGetForUser_Unauthorized() {
        Mockito.when(bookingDao.findById(Mockito.eq(BOOKING_ID)))
                .thenReturn(Optional.of(mockBooking));

        Assert.assertThrows(UnauthorizedException.class, () -> {
            bookingService.getForUser(BOOKING_ID, UUID.randomUUID());
        });
    }
}
