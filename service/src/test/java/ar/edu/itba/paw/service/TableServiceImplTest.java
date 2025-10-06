package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.exceptions.InvalidTableNumberException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
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
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class TableServiceImplTest {
    @Mock
    private ClubDao clubDao;
    @Mock
    private ClubTablesDao clubTablesDao;
    @InjectMocks
    private TableServiceImpl tableService;

    @Test
    public void testFindById_Found() {
        UUID id = UUID.randomUUID();
        Table table = Mockito.mock(Table.class);
        Mockito.when(clubTablesDao.findById(id)).thenReturn(Optional.of(table));
        Optional<Table> result = tableService.findById(id);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(table, result.get());
    }

    @Test
    public void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        Mockito.when(clubTablesDao.findById(id)).thenReturn(Optional.empty());
        Optional<Table> result = tableService.findById(id);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindByClubId_Found() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        List<Table> tables = Arrays.asList(Mockito.mock(Table.class));
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        Mockito.when(clubTablesDao.findByClub(club)).thenReturn(tables);
        List<Table> result = tableService.findByClubId(clubId);
        Assert.assertEquals(tables, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindByClubId_ClubNotFound() {
        UUID clubId = UUID.randomUUID();
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.empty());
        tableService.findByClubId(clubId);
    }

    @Test
    public void testFindAll() {
        List<Table> tables = Arrays.asList(Mockito.mock(Table.class));
        Mockito.when(clubTablesDao.findAll()).thenReturn(tables);
        List<Table> result = tableService.findAll();
        Assert.assertEquals(tables, result);
    }

    @Test
    public void testFindAvailableByClubAndRange_Success() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(club.getClubId()).thenReturn(clubId);
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusHours(2);
        List<Table> tables = Arrays.asList(Mockito.mock(Table.class));
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        Mockito.when(clubTablesDao.findAvailableByClubAndRange(clubId, from, to)).thenReturn(tables);
        List<Table> result = tableService.findAvailableByClubAndRange(clubId, from, to);
        Assert.assertEquals(tables, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindAvailableByClubAndRange_InvalidRange() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        OffsetDateTime from = OffsetDateTime.now();
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
    public void testCreate_Success() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Table table = Mockito.mock(Table.class);
        BigDecimal number = BigDecimal.ONE;
        BigDecimal capacity = BigDecimal.TEN;
        TableState state = TableState.DISPONIBLE;
        BigDecimal price = BigDecimal.valueOf(100);
        BigDecimal discount = BigDecimal.ZERO;
        Mockito.when(clubDao.findById(Mockito.eq(clubId))).thenReturn(Optional.of(club));
        Mockito.when(clubTablesDao.create(
                Mockito.eq(number.intValueExact()),
                Mockito.eq(capacity.intValueExact()),
                Mockito.eq(clubId),
                Mockito.eq(state),
                Mockito.eq(price),
                Mockito.eq(discount)
        )).thenReturn(table);
        Table result = tableService.create(number, capacity, clubId, state, price, discount);
        Assert.assertEquals(table, result);
    }

    @Test(expected = NotFoundException.class)
    public void testCreate_ClubNotFound() {
        UUID clubId = UUID.randomUUID();
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.empty());
        tableService.create(BigDecimal.ONE, BigDecimal.TEN, clubId, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }

    @Test(expected = InvalidTableNumberException.class)
    public void testCreate_InvalidTableNumber() {
        UUID clubId = UUID.randomUUID();
        Club club = Mockito.mock(Club.class);
        Mockito.when(clubDao.findById(clubId)).thenReturn(Optional.of(club));
        tableService.create(BigDecimal.valueOf(-1), BigDecimal.TEN, clubId, TableState.DISPONIBLE, BigDecimal.valueOf(100), BigDecimal.ZERO);
    }
}
