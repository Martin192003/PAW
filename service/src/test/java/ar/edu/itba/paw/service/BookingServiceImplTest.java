package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
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

    // ========================
    // CREATE - HAPPY PATH
    // ========================

    @Test
    public void testCreate_Success() {
        // 1. Setup
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.of(mockTable));
        Mockito.when(bookingDao.findByTable(TABLE_ID)).thenReturn(List.of());
        Mockito.when(bookingDao.create(mockUser, TABLE_ID, Status.PENDIENTE, TOTAL_AMOUNT, START, END))
                .thenReturn(mockBooking);

        // 2. Exercise
        Booking result = bookingService.create(mockUser, TABLE_ID, START, END, TOTAL_AMOUNT);

        // 3. Asserts
        Assert.assertNotNull(result);
        Assert.assertEquals(BOOKING_ID, result.getBookingId());
        Assert.assertEquals(mockUser, result.getUser());
        Assert.assertEquals(TABLE_ID, result.getTableId());
        Assert.assertEquals(Status.PENDIENTE, result.getStatus());
        Assert.assertEquals(START, result.getBookingStart());
        Assert.assertEquals(END, result.getBookingEnd());
        Assert.assertEquals(TOTAL_AMOUNT, result.getTotalAmount());
    }

    // ========================
    // CREATE - UNHAPPY PATHS
    // ========================

    @Test
    public void testCreate_NullUser() {
        // 1. Setup
        // (ningún mock necesario)

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            bookingService.create(null, TABLE_ID, START, END, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreate_TableNotAvailable() {
        // 1. Setup
        mockTable.setTableState(TableState.RESERVADA);
        Mockito.when(tablesDao.findById(Mockito.eq(TABLE_ID)))
                .thenReturn(Optional.of(mockTable));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(TableAlreadyBookedException.class, () -> {
            bookingService.create(mockUser, TABLE_ID, START, END, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreate_OverlapWithActiveBooking() {
        // 1. Setup
        Booking overlapping = new Booking();
        overlapping.setBookingId(UUID.randomUUID());
        overlapping.setBookingStart(START.minusHours(1));
        overlapping.setBookingEnd(END.minusHours(1));
        overlapping.setStatus(Status.PENDIENTE);

        Mockito.when(tablesDao.findById(Mockito.eq(TABLE_ID)))
                .thenReturn(Optional.of(mockTable));
        Mockito.when(bookingDao.findByTable(Mockito.eq(TABLE_ID)))
                .thenReturn(List.of(overlapping));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(TableAlreadyBookedException.class, () -> {
            bookingService.create(mockUser, TABLE_ID, START, END, TOTAL_AMOUNT);
        });
    }

    @Test
    public void testCreate_InvalidDateRange_StartAfterEnd() {
        // 1. Setup
        // (ningún mock necesario)

        // 2. Exercise + 3. Assert
        Assert.assertThrows(InvalidDateRangeException.class, () -> {
            bookingService.create(mockUser, TABLE_ID, END, START, TOTAL_AMOUNT);
        });
    }

    // ========================
    // UPDATE - HAPPY PATH
    // ========================

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
        Mockito.when(tablesDao.findById(TABLE_ID)).thenReturn(Optional.of(mockTable));
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

    // ========================
    // UPDATE STATUS - HAPPY PATH
    // ========================

    @Test
    public void testUpdateStatus_ToConfirmed() {
        // 1. Setup
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

        // 2. Mocks
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

        // 3. Exercise
        Booking result = bookingService.updateStatus(BOOKING_ID, Status.CONFIRMADA);

        // 4. Assert
        Assert.assertEquals(Status.CONFIRMADA, result.getStatus());
    }

    @Test
    public void testUpdateStatus_InvalidTransition() {
        // 1. Setup
        mockBooking.setStatus(Status.CANCELADA);
        Mockito.when(bookingDao.findById(Mockito.eq(BOOKING_ID)))
                .thenReturn(Optional.of(mockBooking));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.updateStatus(BOOKING_ID, Status.PENDIENTE);
        });
    }

    // ========================
    // CANCEL - HAPPY PATH
    // ========================

    @Test
    public void testCancel_Success() {
        // Estado inicial
        mockBooking.setStatus(Status.PENDIENTE);

        // Datos del club
        Club club = new Club();
        club.setClubId(CLUB_ID);

        // Usuario completo (el service lo pide)
        User completeUser = new User();
        completeUser.setUserId(USER_ID);
        completeUser.setEmail("user@example.com");

        // Mocks principales
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

        // Ejecutar
        bookingService.cancel(BOOKING_ID);

        // Assert
        Assert.assertEquals(Status.CANCELADA, mockBooking.getStatus());
    }

    // ========================
    // CONFIRM - HAPPY PATH
    // ========================

    @Test
    public void testConfirm_PendingToConfirmed() {
        // Arrange
        mockBooking.setStatus(Status.PENDIENTE);
        mockBooking.setTableId(TABLE_ID);
        mockBooking.setUser(mockUser);

        // 1) findById inicial
        Mockito.when(bookingDao.findById(BOOKING_ID))
                .thenReturn(Optional.of(mockBooking))        // primera llamada
                .thenReturn(Optional.of(mockBooking));       // segunda llamada dentro del email

        // 2) updateStatus debe modificar el estado
        Mockito.when(bookingDao.updateStatus(BOOKING_ID, Status.CONFIRMADA))
                .thenAnswer(inv -> {
                    mockBooking.setStatus(Status.CONFIRMADA);
                    return Optional.of(mockBooking);
                });

        // 3) ensureTableExists → usa tablesDao
        Mockito.when(tablesDao.findById(TABLE_ID))
                .thenReturn(Optional.of(mockTable));

        // 4) clubDao
        Club club = new Club();
        club.setClubId(CLUB_ID);
        Mockito.when(clubDao.findById(CLUB_ID))
                .thenReturn(Optional.of(club));

        // 5) userService
        mockUser.setEmail("mail@test.com");
        mockUser.setLocale("es");
        Mockito.when(userService.findById(USER_ID))
                .thenReturn(Optional.of(mockUser));

        // 6) email no hace nada
        Mockito.doNothing().when(emailService)
                .notifyConfirmationEmailToUser(any(), any(), any(), any());

        // Act
        bookingService.confirm(BOOKING_ID);

        // Assert
        Assert.assertEquals(Status.CONFIRMADA, mockBooking.getStatus());
    }


    @Test
    public void testConfirm_AlreadyCancelled() {
        // 1. Setup
        mockBooking.setStatus(Status.CANCELADA);
        Mockito.when(bookingDao.findById(Mockito.eq(BOOKING_ID)))
                .thenReturn(Optional.of(mockBooking));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.confirm(BOOKING_ID);
        });
    }

    // ========================
    // FIND BY ID
    // ========================

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

    // ========================
    // GET FOR USER - AUTH
    // ========================

    @Test
    public void testGetForUser_Authorized() {
        Mockito.when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(mockBooking));

        Booking result = bookingService.getForUser(BOOKING_ID, USER_ID);

        Assert.assertEquals(mockBooking, result);
    }

    @Test
    public void testGetForUser_Unauthorized() {
        // 1. Setup
        Mockito.when(bookingDao.findById(Mockito.eq(BOOKING_ID)))
                .thenReturn(Optional.of(mockBooking));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(UnauthorizedException.class, () -> {
            bookingService.getForUser(BOOKING_ID, UUID.randomUUID());
        });
    }
}
