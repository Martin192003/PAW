package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.model.exceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingDao bookingDao;
    private final ClubTablesDao tablesDao;

    public BookingServiceImpl(final BookingDao bookingDao, final ClubTablesDao tablesDao) {
        this.bookingDao = bookingDao;
        this.tablesDao = tablesDao;
    }

    @Override
    @Transactional
    public Booking create(final User user,
                          final UUID tableId,
                          final OffsetDateTime bookingStart,
                          final OffsetDateTime bookingEnd,
                          final BigDecimal totalAmount) {

        Assert.notNull(user, "user no puede ser null");
        Assert.notNull(tableId, "tableId no puede ser null");
        Assert.notNull(bookingStart, "bookingStart no puede ser null");
        Assert.notNull(bookingEnd, "bookingEnd no puede ser null");
        Assert.notNull(totalAmount, "totalAmount no puede ser null");

        validateDateRange(bookingStart, bookingEnd);
        final Table table = ensureTableExists(tableId);

        if (table.getTableState() != null && table.getTableState() != TableState.DISPONIBLE) {
            throw new TableAlreadyBookedException();
        }

        ensureNoOverlap(tableId, bookingStart, bookingEnd, null);

        final Status initial = Status.PENDIENTE;
        final Booking booking = bookingDao.create(user, tableId, initial, totalAmount, bookingStart, bookingEnd);

        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Booking> findById(UUID bookingId) {
        return bookingDao.findById(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByUser(UUID userId) {
        return bookingDao.findByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findByTable(UUID tableId) {
        return bookingDao.findByTable(tableId);
    }

    @Override
    @Transactional
    public Booking update(UUID bookingId,
                          User user,
                          UUID tableId,
                          OffsetDateTime bookingStart,
                          OffsetDateTime bookingEnd,
                          BigDecimal totalAmount,
                          Status status) {

        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        Assert.notNull(user, "user no puede ser null");
        Assert.notNull(tableId, "tableId no puede ser null");
        Assert.notNull(bookingStart, "bookingStart no puede ser null");
        Assert.notNull(bookingEnd, "bookingEnd no puede ser null");
        Assert.notNull(totalAmount, "totalAmount no puede ser null");
        Assert.notNull(status, "status no puede ser null");

        validateDateRange(bookingStart, bookingEnd);
        final Table table = ensureTableExists(tableId);

        if (status != Status.DISPONIBLE) {
            ensureNoOverlap(tableId, bookingStart, bookingEnd, bookingId);
        }
        validateStatusTransition(existing.getStatus(), status);

        final Booking updated = bookingDao.update(bookingId, user, tableId, status, totalAmount, bookingStart, bookingEnd);

        return updated;
    }

    @Override
    @Transactional
    public Booking updateStatus(UUID bookingId, Status newStatus) {
        final Booking current = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        validateStatusTransition(current.getStatus(), newStatus);

        final Booking updated = bookingDao.update(
                bookingId,
                current.getUser(),
                current.getTableId(),
                newStatus,
                current.getTotalAmount(),
                current.getBookingStart(),
                current.getBookingEnd()
        );


        return updated;
    }

    @Override
    @Transactional
    public boolean cancel(UUID bookingId) {
        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        validateStatusTransition(existing.getStatus(), Status.CANCELADA);

        bookingDao.update(
                bookingId,
                existing.getUser(),
                existing.getTableId(),
                Status.CANCELADA,
                existing.getTotalAmount(),
                existing.getBookingStart(),
                existing.getBookingEnd()
        );

        return true;
    }

    @Override
    public boolean delete(UUID bookingId) {
        return bookingDao.delete(bookingId);
    }



    private void validateDateRange(OffsetDateTime start, OffsetDateTime end) {
        if (!start.isBefore(end)) throw new InvalidDateRangeException("Rango inválido: " + start + " - " + end);
    }

    private Table ensureTableExists(UUID tableId) {
        return tablesDao.findById(tableId).orElseThrow(() -> new TableNotFoundException(tableId));
    }


    private void ensureNoOverlap(UUID tableId,
                                 OffsetDateTime start,
                                 OffsetDateTime end,
                                 UUID excludeBookingId) {

        final List<Booking> bookings = bookingDao.findByTable(tableId);
        final boolean overlaps = bookings.stream()
                .filter(b -> excludeBookingId == null || !b.getBookingId().equals(excludeBookingId))
                .anyMatch(b ->
                        b.getStatus() != Status.DISPONIBLE &&
                                b.getBookingStart().isBefore(end) &&
                                b.getBookingEnd().isAfter(start)
                );

        if (overlaps) throw new TableAlreadyBookedException();
    }

    private void maybeFreeTable(Table table) {
        final List<Booking> bookings = bookingDao.findByTable(table.getTableId());
        final boolean stillBlocked = bookings.stream()
                .anyMatch(b -> b.getStatus() == Status.PENDIENTE || b.getStatus() == Status.CONFIRMADA);

        if (!stillBlocked) {
            tablesDao.update(
                    table.getTableId(),
                    table.getClubId(),
                    table.getTableNumber(),
                    table.getCapacity(),
                    TableState.DISPONIBLE,
                    table.getPrice(),
                    table.getDiscount()
            );
        }
    }

    /**
     * Reglas con tus estados:
     * - DISPONIBLE  -> PENDIENTE | RESERVADA
     * - PENDIENTE   -> RESERVADA | DISPONIBLE
     * - RESERVADA   -> DISPONIBLE
     */
    private void validateStatusTransition(Status from, Status to) {
        if (from == to) return;

        switch (from) {
            case DISPONIBLE -> {
                if (!(to == Status.PENDIENTE || to == Status.CONFIRMADA)) {
                    throw new InvalidStatusTransitionException(from, to);
                }
            }
            case PENDIENTE -> {
                if (!(to == Status.CONFIRMADA || to == Status.DISPONIBLE || to == Status.CANCELADA)) {
                    throw new InvalidStatusTransitionException(from, to);
                }
            }
            case CONFIRMADA -> {
                if (!(to == Status.PENDIENTE || to == Status.CANCELADA)) {
                    throw new InvalidStatusTransitionException(from, to);
                }
            }
            case CANCELADA -> {
                throw new InvalidStatusTransitionException(from, to);
            }
            default -> throw new InvalidStatusTransitionException(from, to);
        }
    }
}
