package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableService {
    Optional<ClubTable> findById(UUID id);
    List<ClubTable> findByClubId(UUID clubId);
    List<ClubTable> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to);
    Optional<ClubTable> findByBookingId(UUID bookingId);

    ClubTable create(BigDecimal number, BigDecimal capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount);
    ClubTable update(UUID tableId, UUID clubId, BigDecimal tableNumber, BigDecimal capacity, TableState state, BigDecimal price, BigDecimal discount);
    boolean delete(UUID id);

    List<ClubTable> findAvailableOnDate(UUID clubId, LocalDate date, ZoneId tz, Clock clock);

    boolean isAllowedDay(LocalDate date);
    boolean isPastDate(LocalDate date, Clock clock, ZoneId tz);
    boolean canAddTable(UUID clubId);

    OffsetDateTime windowStart(LocalDate date, ZoneId tz);
    OffsetDateTime windowEnd(LocalDate date, ZoneId tz);
    LocalDate getNextAllowedDate(LocalDate from);
    List<LocalDate> getAllowedBookingDates(LocalDate startDate, int numberOfDays);
    LocalDate resolveSelectedDate(LocalDate date, boolean fromEvent);
    List<String> getAllowedDateStrings(LocalDate baseDate, int days);

}
