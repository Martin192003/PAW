package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class TableServiceImplTest {

    @Mock
    private ClubDao clubDao;
    @Mock
    private ClubTablesDao clubTablesDao;
    @InjectMocks
    private TableServiceImpl tableService;

    private final ZoneId testZone = ZoneId.of("America/Argentina/Buenos_Aires");
    private final LocalDate fixedLocalDate = LocalDate.of(2025, 11, 14);
    private final Instant fixedInstant = fixedLocalDate.atStartOfDay(testZone).toInstant();

    @Before
    public void setUp() throws Exception {
        Clock fixedClock = Clock.fixed(fixedInstant, testZone);
        Field clockField = TableServiceImpl.class.getDeclaredField("clock");
        clockField.setAccessible(true);
        clockField.set(tableService, fixedClock);
    }

    @Test
    public void testFindByIdFound() {
        UUID id = UUID.randomUUID();
        ClubTable table = Mockito.mock(ClubTable.class);
        Mockito.when(clubTablesDao.findById(id)).thenReturn(Optional.of(table));

        Optional<ClubTable> result = tableService.findById(id);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(table, result.get());
    }

    @Test
    public void testFindByIdNotFound() {
        UUID id = UUID.randomUUID();
        Mockito.when(clubTablesDao.findById(id)).thenReturn(Optional.empty());

        Optional<ClubTable> result = tableService.findById(id);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindByClubIdFound() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        List<ClubTable> tables = Arrays.asList(Mockito.mock(ClubTable.class));
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        Mockito.when(clubTablesDao.findByClub(club)).thenReturn(tables);

        List<ClubTable> result = tableService.findByClubId(clubId);

        Assert.assertEquals(tables, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindByClubIdClubNotFound() {
        UUID clubId = UUID.randomUUID();
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.empty());
        tableService.findByClubId(clubId);
    }

    @Test
    public void testFindAvailableByClubAndRangeSuccess() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        OffsetDateTime from = ZonedDateTime.of(2025, 11, 14, 23, 59, 0, 0, testZone).toOffsetDateTime();
        OffsetDateTime to = from.plusHours(2);
        List<ClubTable> tables = Arrays.asList(Mockito.mock(ClubTable.class));

        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        Mockito.when(club.getClubId()).thenReturn(clubId);
        Mockito.when(clubTablesDao.findAvailableByClubAndRange(clubId, from, to)).thenReturn(tables);

        List<ClubTable> result = tableService.findAvailableByClubAndRange(clubId, from, to);

        Assert.assertEquals(tables, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindAvailableByClubAndRangeInvalidRange() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        OffsetDateTime from = OffsetDateTime.now().plusHours(2);
        OffsetDateTime to = from.minusHours(1);
        tableService.findAvailableByClubAndRange(clubId, from, to);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindAvailableByClubAndRange_ClubNotFound() {
        UUID clubId = UUID.randomUUID();
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.empty());
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusHours(1);
        tableService.findAvailableByClubAndRange(clubId, from, to);
    }

    @Test
    public void testCreateSuccess() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        ClubTable table = Mockito.mock(ClubTable.class);

        BigDecimal number = BigDecimal.ONE;
        BigDecimal capacity = BigDecimal.TEN;
        TableState state = TableState.DISPONIBLE;
        BigDecimal price = BigDecimal.valueOf(100);
        BigDecimal discount = BigDecimal.ZERO;

        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        Mockito.when(clubTablesDao.create(
                Mockito.eq(number.intValueExact()),
                Mockito.eq(capacity.intValueExact()),
                Mockito.eq(clubId),
                Mockito.eq(state),
                Mockito.eq(price),
                Mockito.eq(discount)
        )).thenReturn(table);

        ClubTable result = tableService.create(number, capacity, clubId, state, price, discount);

        Assert.assertEquals(table, result);
    }

    @Test(expected = NotFoundException.class)
    public void testCreateClubNotFound() {
        UUID clubId = UUID.randomUUID();
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.empty());
        tableService.create(BigDecimal.ONE, BigDecimal.TEN, clubId, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test(expected = InvalidTableNumberException.class)
    public void testCreateInvalidTableNumber() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        tableService.create(BigDecimal.valueOf(-1), BigDecimal.TEN, clubId, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test(expected = InvalidCapacityException.class)
    public void testCreateInvalidCapacity() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        tableService.create(BigDecimal.ONE, BigDecimal.ZERO, clubId, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test
    public void testUpdateSuccess() {
        UUID tableId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        ClubTable table = Mockito.mock(ClubTable.class);

        BigDecimal tableNumber = BigDecimal.ONE;
        BigDecimal capacity = BigDecimal.TEN;
        TableState state = TableState.DISPONIBLE;
        BigDecimal price = BigDecimal.valueOf(100);
        BigDecimal discount = BigDecimal.ZERO;

        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        Mockito.when(clubTablesDao.findByClub(club)).thenReturn(Collections.nCopies(10, Mockito.mock(ClubTable.class)));
        Mockito.when(clubTablesDao.update(tableId, clubId, tableNumber.intValueExact(), capacity.intValueExact(), state, price, discount)).thenReturn(table);

        ClubTable result = tableService.update(tableId, clubId, tableNumber, capacity, state, price, discount);

        Assert.assertEquals(table, result);
    }

    @Test(expected = NotFoundException.class)
    public void testUpdateClubNotFound() {
        UUID tableId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.empty());

        tableService.update(tableId, clubId, BigDecimal.ONE, BigDecimal.TEN, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test(expected = InvalidTableNumberException.class)
    public void testUpdateInvalidTableNumber() {
        UUID tableId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));

        tableService.update(tableId, clubId, BigDecimal.valueOf(-5), BigDecimal.TEN, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateCannotAddTableWhenMaxReached() {
        UUID tableId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);

        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        List<ClubTable> fifty = new ArrayList<>();
        for (int i = 0; i < 50; i++) fifty.add(Mockito.mock(ClubTable.class));
        Mockito.when(clubTablesDao.findByClub(club)).thenReturn(fifty);

        tableService.update(tableId, clubId, BigDecimal.ONE, BigDecimal.TEN, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test
    public void testDeleteSuccessTrue() {
        UUID id = UUID.randomUUID();
        Mockito.when(clubTablesDao.delete(id)).thenReturn(true);
        boolean result = tableService.delete(id);
        Assert.assertTrue(result);
    }

    @Test
    public void testDeleteSuccessFalse() {
        UUID id = UUID.randomUUID();
        Mockito.when(clubTablesDao.delete(id)).thenReturn(false);
        boolean result = tableService.delete(id);
        Assert.assertFalse(result);
    }

    @Test
    public void testFindAvailableOnDateSuccess() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));

        LocalDate date = fixedLocalDate;
        OffsetDateTime from = tableService.windowStart(date, testZone);
        OffsetDateTime to = tableService.windowEnd(date, testZone);

        List<ClubTable> expected = Arrays.asList(Mockito.mock(ClubTable.class));
        Mockito.when(clubTablesDao.findAvailableByClubAndRange(club.getClubId(), from, to)).thenReturn(expected);

        List<ClubTable> result = tableService.findAvailableOnDate(clubId, date, testZone, Clock.fixed(fixedInstant, testZone));

        Assert.assertEquals(expected, result);
    }

    @Test(expected = ValidationException.class)
    public void testFindAvailableOnDatePastDateThrows() {
        UUID clubId = UUID.randomUUID();
        LocalDate past = fixedLocalDate.minusDays(2);
        tableService.findAvailableOnDate(clubId, past, testZone, Clock.fixed(fixedInstant, testZone));
    }

    @Test
    public void testIsAllowedDay() {
        LocalDate thursday = LocalDate.of(2025, 11, 13);
        LocalDate sunday = LocalDate.of(2025, 11, 16);

        Assert.assertTrue(tableService.isAllowedDay(thursday));
        Assert.assertFalse(tableService.isAllowedDay(sunday));
    }

    @Test
    public void testIsPastDate() {
        LocalDate before = fixedLocalDate.minusDays(1);
        LocalDate same = fixedLocalDate;
        Clock c = Clock.fixed(fixedInstant, testZone);

        Assert.assertTrue(tableService.isPastDate(before, c, testZone));
        Assert.assertFalse(tableService.isPastDate(same, c, testZone));
    }

    @Test
    public void testWindowStartAndEnd() {
        LocalDate d = LocalDate.of(2025, 11, 14);
        OffsetDateTime start = tableService.windowStart(d, testZone);
        OffsetDateTime end = tableService.windowEnd(d, testZone);

        Assert.assertEquals(23, start.getHour());
        Assert.assertEquals(59, start.getMinute());

        Assert.assertEquals(6, end.getHour());
        Assert.assertEquals(0, end.getMinute());
        Assert.assertEquals(d.plusDays(1), end.toLocalDate());
    }

    @Test
    public void testCanAddTableWhenBelowLimit() {
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubTablesDao.findByClub(club)).thenReturn(Collections.nCopies(10, Mockito.mock(ClubTable.class)));
        Assert.assertTrue(tableService.canAddTable(club));
    }

    @Test
    public void testCanAddTableWhenAtLimit() {
        Club club = Mockito.mock(Club.class);
        List<ClubTable> fifty = new ArrayList<>();
        for (int i = 0; i < 50; i++) fifty.add(Mockito.mock(ClubTable.class));
        Mockito.when(clubTablesDao.findByClub(club)).thenReturn(fifty);
        Assert.assertFalse(tableService.canAddTable(club));
    }


    @Test
    public void testGetAllowedBookingDates() {
        LocalDate start = fixedLocalDate;
        List<LocalDate> allowed = tableService.getAllowedBookingDates(start, 3);

        Assert.assertTrue(allowed.contains(LocalDate.of(2025, 11, 14)));
        Assert.assertTrue(allowed.contains(LocalDate.of(2025, 11, 15)));
        Assert.assertFalse(allowed.contains(LocalDate.of(2025, 11, 16)));
    }

    @Test
    public void testGetNextAllowedDate() {
        LocalDate from = LocalDate.of(2025, 11, 13);
        LocalDate next = tableService.getNextAllowedDate(from);
        Assert.assertEquals(LocalDate.of(2025, 11, 14), next);
    }

    @Test
    public void testResolveSelectedDateFromEventKeepsDate() {
        LocalDate input = LocalDate.of(2025, 11, 20);
        LocalDate result = tableService.resolveSelectedDate(input, true);
        Assert.assertEquals(input, result);
    }

    @Test
    public void testResolveSelectedDateDefaultsToNextAllowed() {
        LocalDate result = tableService.resolveSelectedDate(null, false);
        LocalDate expected = tableService.getNextAllowedDate(LocalDate.now());
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testGetAllowedDateStrings() {
        LocalDate base = fixedLocalDate;
        List<String> strings = tableService.getAllowedDateStrings(base, 3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> expected = tableService.getAllowedBookingDates(base, 3).stream().map(d -> d.format(formatter)).toList();

        Assert.assertEquals(expected, strings);
    }
}
