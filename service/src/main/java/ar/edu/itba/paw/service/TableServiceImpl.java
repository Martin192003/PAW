package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class TableServiceImpl implements TableService {

    private static final Set<DayOfWeek> ALLOWED = Set.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);


    private final ClubDao clubDao;
    private final ClubTablesDao clubTablesDao;

    public TableServiceImpl(ClubTablesDao clubTablesDao, ClubDao clubDao) {
        this.clubTablesDao = clubTablesDao;
        this.clubDao = clubDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Table> findById(UUID id) {
        return clubTablesDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findByClubId(UUID clubId) {
        var club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        var list = clubTablesDao.findByClub(club);
        return list;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll() {
        return clubTablesDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to) {
        // valida club y normaliza rango
        var club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        if (!from.isBefore(to)) throw new IllegalArgumentException("Rango inválido");
        return clubTablesDao.findAvailableByClubAndRange(club.getClubId(), from, to);
    }

    @Override
    @Transactional
    public Table create(BigDecimal number, BigDecimal capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount) {
        if (clubDao.findById(clubId).isEmpty()) throw new NotFoundException("Club no existente");
        if ( number == null || number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(BigDecimal.valueOf(1_000_000)) >=  0) throw new InvalidTableNumberException("Número de mesa inválido");
        if ( capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0 || capacity.compareTo(BigDecimal.valueOf(10_000)) >= 0) throw new InvalidCapacityException("Capacidad inválida");
        if (price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(new BigDecimal("99999999.99")) >= 0) throw new InvalidPriceException("Precio inválido");
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) throw new ValidationException("Descuento inválido");
        return clubTablesDao.create(number.intValueExact(), capacity.intValueExact(), clubId, tableState, price, discount);
    }

    @Override
    @Transactional
    public Table update(UUID tableId, UUID clubId, BigDecimal tableNumber, BigDecimal capacity, TableState state, BigDecimal price, BigDecimal discount) {
        if (clubDao.findById(clubId).isEmpty()) throw new NotFoundException("Club no existente");
        if ( tableNumber == null || tableNumber.compareTo(BigDecimal.ZERO) < 0 || tableNumber.compareTo(BigDecimal.valueOf(1_000_000)) >=  0) throw new InvalidTableNumberException("Número de mesa inválido");
        if ( capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0 || capacity.compareTo(BigDecimal.valueOf(10_000)) >= 0) throw new InvalidCapacityException("Capacidad inválida");
        if (price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(new BigDecimal("99999999.99")) >= 0) throw new InvalidPriceException("Precio inválido");
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) throw new ValidationException("Descuento inválido");
        return clubTablesDao.update(tableId, clubId, tableNumber.intValueExact(), capacity.intValueExact(), state, price, discount);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        return clubTablesDao.delete(id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Table> findAvailableOnDate(UUID clubId, LocalDate date, ZoneId tz, Clock clock) {
        if (!isAllowedDay(date)) {
            throw new ValidationException("Fecha no permitida para reservas");
        }
        if (isPastDate(date, clock, tz)) {
            throw new ValidationException("Fecha en el pasado");
        }
        var from = windowStart(date, tz);
        var to   = windowEnd(date, tz);
        return findAvailableByClubAndRange(clubId, from, to);
    }

    @Override
    public boolean isAllowedDay(LocalDate date) {
        return ALLOWED.contains(date.getDayOfWeek());
    }

    @Override
    public boolean isPastDate(LocalDate date, Clock clock, ZoneId tz) {
        var today = ZonedDateTime.now(clock).withHour(0).withMinute(0).withSecond(0).withNano(0).toLocalDate();
        return date.isBefore(today);
    }

    @Override
    public OffsetDateTime windowStart(LocalDate date, ZoneId tz) {
        return date.atTime(23,59).atZone(tz).toOffsetDateTime();
    }

    @Override
    public OffsetDateTime windowEnd(LocalDate date, ZoneId tz) {
        return date.plusDays(1).atTime(6,0).atZone(tz).toOffsetDateTime();
    }

}
