package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TableServiceImpl implements TableService {

    private static final Set<DayOfWeek> ALLOWED = Set.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    private static final int MAX_TABLES_PER_CLUB = 50;
    private final ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
    private final Clock clock = Clock.system(zone);

    private final ClubDao clubDao;
    private final ClubTablesDao clubTablesDao;
    private final BookingDao bookingDao;

    public TableServiceImpl(ClubTablesDao clubTablesDao, ClubDao clubDao, BookingDao bookingDao) {
        this.clubTablesDao = clubTablesDao;
        this.clubDao = clubDao;
        this.bookingDao = bookingDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubTable> findById(UUID id) {
        return clubTablesDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubTable> findByClubId(UUID clubId) {
        var club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        var list = clubTablesDao.findByClub(club);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClubTable> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to) {
        var club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        if (!from.isBefore(to)) throw new IllegalArgumentException("Rango inválido");
        return clubTablesDao.findAvailableByClubAndRange(club.getClubId(), from, to);
    }

    @Override
    @Transactional
    public ClubTable create(BigDecimal number, BigDecimal capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount) {
        if (clubDao.findById(clubId).isEmpty()) throw new NotFoundException("Club no existente");
        if ( number == null || number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(BigDecimal.valueOf(1_000_000)) >=  0) throw new InvalidTableNumberException("Número de mesa inválido");
        if ( capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0 || capacity.compareTo(BigDecimal.valueOf(10_000)) >= 0) throw new InvalidCapacityException("Capacidad inválida");
        if (price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(new BigDecimal("99999999.99")) >= 0) throw new InvalidPriceException("Precio inválido");
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) throw new ValidationException("Descuento inválido");
        return clubTablesDao.create(number.intValueExact(), capacity.intValueExact(), clubId, tableState, price, discount);
    }

    @Override
    @Transactional
    public ClubTable update(UUID tableId, UUID clubId, BigDecimal tableNumber, BigDecimal capacity, TableState state, BigDecimal price, BigDecimal discount) {
        if (clubDao.findById(clubId).isEmpty()) throw new NotFoundException("Club no existente");
        if ( tableNumber == null || tableNumber.compareTo(BigDecimal.ZERO) < 0 || tableNumber.compareTo(BigDecimal.valueOf(1_000_000)) >=  0) throw new InvalidTableNumberException("Número de mesa inválido");
        if ( capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0 || capacity.compareTo(BigDecimal.valueOf(10_000)) >= 0) throw new InvalidCapacityException("Capacidad inválida");
        if (price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(new BigDecimal("99999999.99")) >= 0) throw new InvalidPriceException("Precio inválido");
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) throw new ValidationException("Descuento inválido");
        if (!canAddTable(clubId)) throw new IllegalStateException("Numero maximo de mesas alcanzado");
        return clubTablesDao.update(tableId, clubId, tableNumber.intValueExact(), capacity.intValueExact(), state, price, discount);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        return clubTablesDao.delete(id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<ClubTable> findAvailableOnDate(UUID clubId, LocalDate date, ZoneId tz, Clock clock) {
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

    @Override
    @Transactional(readOnly = true)
    public boolean canAddTable(UUID clubId) {
        Club club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        Objects.requireNonNull(club, "club is required");
        int count = clubTablesDao.findByClub(club).size();
        return count < MAX_TABLES_PER_CLUB;
    }

    @Override
    public List<LocalDate> getAllowedBookingDates(LocalDate startDate, int numberOfDays) {
        List<LocalDate> allowedDates = new ArrayList<>();
        for (int i = 0; i < numberOfDays; i++) {
            LocalDate d = startDate.plusDays(i);
            DayOfWeek day = d.getDayOfWeek();
            if ((day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) && !isPastDate(d, clock, zone)) {
                allowedDates.add(d);
            }
        }
        return allowedDates;
    }

    @Override
    public LocalDate getNextAllowedDate(LocalDate from) {
        LocalDate d = from;
        int guard = 0;
        while (!isAllowedDay(d) || isPastDate(d, clock, zone)) {
            d = d.plusDays(1);
            if (++guard > 14) break;
        }
        return d;
    }

    @Override
    public LocalDate resolveSelectedDate(LocalDate date, boolean fromEvent) {
        if (fromEvent && date != null) {
            return date;
        }
        LocalDate defaultDate = getNextAllowedDate(LocalDate.now());
        return (date == null) ? defaultDate : date;
    }


    @Override
    public List<String> getAllowedDateStrings(LocalDate baseDate, int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<LocalDate> allowedDates = getAllowedBookingDates(baseDate, days);
        return allowedDates.stream()
                .map(d -> d.format(formatter))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubTable> findByBookingId(UUID bookingId){
        Booking booking = bookingDao.findById(bookingId).orElseThrow(InvalidBookingStateException::new);
        return findById(booking.getTableId());
    }


}
