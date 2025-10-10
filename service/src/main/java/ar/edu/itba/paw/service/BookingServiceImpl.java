package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Set<DayOfWeek> ALLOWED_DAYS =
            EnumSet.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private final BookingDao bookingDao;
    private final ClubTablesDao tablesDao;
    private final UserService userService;
    private final ClubService clubService;
    private final TableService tableService;
    private final ClubDao clubDao;
    private final EmailService emailService;

    public BookingServiceImpl(final BookingDao bookingDao,
                              final ClubTablesDao tablesDao,
                              final UserService userService,
                              final ClubService clubService,
                              final TableService tableService,
                              final ClubDao clubDao,
                              final EmailService emailService) {
        this.bookingDao = bookingDao;
        this.tablesDao = tablesDao;
        this.userService = userService;
        this.clubService = clubService;
        this.tableService = tableService;
        this.clubDao = clubDao;
        this.emailService = emailService;
    }

    @Override
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

        // FIX: faltaba el parámetro
        final ClubTable table = ensureTableExists(tableId);

        if (table.getTableState() != null && table.getTableState() != TableState.DISPONIBLE) {
            throw new TableAlreadyBookedException();
        }

        ensureNoOverlap(tableId, bookingStart, bookingEnd, null);

        final Status initial = Status.PENDIENTE;

        final Booking booking = bookingDao.create(user, tableId, initial, totalAmount, bookingStart, bookingEnd);

        LOGGER.info("Booking creado id={} user={} table={} {}-{}",
                booking.getBookingId(), user.getId(), tableId, bookingStart, bookingEnd);

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

        final ClubTable table = ensureTableExists(tableId);

        // Chequear solapamiento sólo si va a quedar 'activa'
        if (status == Status.PENDIENTE || status == Status.CONFIRMADA) {
            ensureNoOverlap(tableId, bookingStart, bookingEnd, bookingId);
        }

        validateStatusTransition(existing.getStatus(), status);

        // Si tenés relación @ManyToOne, preferí pasar la entidad 'table'
        return bookingDao.update(bookingId, user, tableId, status, totalAmount, bookingStart, bookingEnd)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
    }

    @Override
    public Booking updateStatus(UUID bookingId, Status newStatus) {
        final Booking current = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        validateStatusTransition(current.getStatus(), newStatus);

        // Si pasa a activa (PENDIENTE/CONFIRMADA) no cambiamos fechas ni mesa,
        // por lo que no hay chequeo de overlap adicional aquí.
        final Booking updated = bookingDao.update(
                        bookingId,
                        current.getUser(),
                        current.getTableId(),
                        newStatus,
                        current.getTotalAmount(),
                        current.getBookingStart(),
                        current.getBookingEnd()
                )
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        if (newStatus == Status.CANCELADA) {
            // Liberar mesa en caso de quedar sin reservas activas
            try {
                final ClubTable table = ensureTableExists(updated.getTableId());
                maybeFreeTable(table);
            } catch (TableNotFoundException ignored) {
                // Si no está la mesa, no hay nada que liberar
            }
        }

        return updated;
    }

    @Override
    public void cancel(UUID bookingId) {
        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        validateStatusTransition(existing.getStatus(), Status.CANCELADA);

        final ClubTable table = ensureTableExists(existing.getTableId());
        final Club club = clubDao.findById(table.getClubId()).orElseThrow();

        final User completeUser = userService.findById(existing.getUser().getId());
        if (completeUser == null) {
            throw new UserNotFoundException("Usuario no encontrado: " + existing.getUser().getId());
        }

        LOGGER.info("Cancel booking={} by user id={} email={}",
                existing.getBookingId(), completeUser.getId(), completeUser.getEmail());

        bookingDao.update(
                bookingId,
                existing.getUser(),
                existing.getTableId(),
                Status.CANCELADA,
                existing.getTotalAmount(),
                existing.getBookingStart(),
                existing.getBookingEnd()
        );

        Booking bookingForEmail = new Booking();
        bookingForEmail.setBookingId(existing.getBookingId());
        bookingForEmail.setUser(completeUser);
        bookingForEmail.setTable(existing.getTable());
        bookingForEmail.setBookingStart(existing.getBookingStart());
        bookingForEmail.setBookingEnd(existing.getBookingEnd());
        bookingForEmail.setStatus(Status.CANCELADA);
        bookingForEmail.setTotalAmount(existing.getTotalAmount());

        Locale locale = new Locale("es", "AR");

        try {
            emailService.notifyBookingCancelledToUser(bookingForEmail, club, table, locale);
        } catch (Exception e) {
            LOGGER.error("Error enviando email de cancelación al usuario para booking {}", bookingId, e);
        }

        try {
            List<User> admins = clubService.getAdminsByClub(club.getClubId());
            if (!admins.isEmpty() && admins.get(0).getEmail() != null && !admins.get(0).getEmail().trim().isEmpty()) {
                emailService.notifyBookingCancelledToOwner(bookingForEmail, club, table, admins.get(0), locale);
            } else {
                LOGGER.warn("No hay email de owner válido para club {}", club.getClubId());
            }
        } catch (Exception e) {
            LOGGER.error("Error enviando email de cancelación al dueño para booking {}", bookingId, e);
        }

        maybeFreeTable(table);
    }

    @Override
    public void cancelByUser(UUID bookingId, UUID userId) {
        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        if (!existing.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("El usuario no puede cancelar esta reserva");
        }

        validateStatusTransition(existing.getStatus(), Status.CANCELADA);

        final ClubTable table = ensureTableExists(existing.getTableId());
        final Club club = clubDao.findById(table.getClubId()).orElseThrow();

        final User completeUser = userService.findById(existing.getUser().getId());
        if (completeUser == null) {
            throw new UserNotFoundException("Usuario no encontrado: " + existing.getUser().getId());
        }

        bookingDao.update(
                bookingId,
                existing.getUser(),
                existing.getTableId(),
                Status.CANCELADA,
                existing.getTotalAmount(),
                existing.getBookingStart(),
                existing.getBookingEnd()
        );

        Booking bookingForEmail = new Booking();
        bookingForEmail.setBookingId(existing.getBookingId());
        bookingForEmail.setUser(completeUser);
        bookingForEmail.setTable(existing.getTable());
        bookingForEmail.setBookingStart(existing.getBookingStart());
        bookingForEmail.setBookingEnd(existing.getBookingEnd());
        bookingForEmail.setStatus(Status.CANCELADA);
        bookingForEmail.setTotalAmount(existing.getTotalAmount());

        Locale locale = new Locale("es", "AR");

        try {
            emailService.notifyBookingCancelledToUser(bookingForEmail, club, table, locale);
        } catch (Exception e) {
            LOGGER.error("Error enviando email de cancelación al usuario para booking {}: {}", bookingId, e.getMessage(), e);
        }

        try {
            List<User> admins = clubService.getAdminsByClub(club.getClubId());
            if (!admins.isEmpty() && admins.get(0).getEmail() != null && !admins.get(0).getEmail().trim().isEmpty()) {
                emailService.notifyBookingCancelledToOwner(bookingForEmail, club, table, admins.get(0), locale);
            } else {
                LOGGER.warn("No hay email de owner válido para club {}", club.getClubId());
            }
        } catch (Exception e) {
            LOGGER.error("Error enviando email de cancelación al dueño para booking {}", bookingId, e);
        }

        maybeFreeTable(table);
    }

    @Override
    public void confirm(UUID bookingId) {
        Booking b = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("booking.notfound"));

        if (b.getStatus() == Status.CANCELADA) {
            throw new InvalidStatusTransitionException(b.getStatus(), Status.CONFIRMADA);
        }
        if (b.getStatus() == Status.CONFIRMADA) {
            return;
        }
        if (b.getStatus() != Status.PENDIENTE) {
            throw new InvalidStatusTransitionException(b.getStatus(), Status.CONFIRMADA);
        }

        bookingDao.updateStatus(bookingId, Status.CONFIRMADA)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        try {
            final Booking updated = bookingDao.findById(bookingId).orElseThrow();
            final ClubTable table = ensureTableExists(updated.getTableId());
            final Club club = clubDao.findById(table.getClubId()).orElseThrow();
            Locale locale = new Locale("es", "AR");
            emailService.notifyConfirmationEmailToUser(updated, club, table, locale);
        } catch (Exception e) {
            LOGGER.error("Error sending confirmation email for booking {}", bookingId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getForUser(UUID bookingId, UUID userId) {
        final Booking b = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        if (b.getUser() == null || b.getUser().getId() == null || !b.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }
        return b;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchForAdminPaginated(UUID adminUserId,
                                                 UUID clubId,
                                                 UUID bookingId,
                                                 Status status,
                                                 String sort,
                                                 int limit,
                                                 int offset) {

        List<UUID> adminClubIds = clubDao.findIdsByAdmin(adminUserId);
        if (adminClubIds == null || adminClubIds.isEmpty()) {
            return List.of();
        }

        if (clubId != null) {
            if (!adminClubIds.contains(clubId)) {
                // También podrías lanzar AccessDenied/Unauthorized si preferís
                return List.of();
            }
            adminClubIds = List.of(clubId);
        }

        return bookingDao.searchByClubIdsPaginated(adminClubIds, bookingId, status, sort, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearchForAdmin(UUID adminUserId,
                                   UUID clubId,
                                   UUID bookingId,
                                   Status status) {

        List<UUID> adminClubIds = clubDao.findIdsByAdmin(adminUserId);
        if (adminClubIds == null || adminClubIds.isEmpty()) {
            throw  new UnauthorizedException("booking.notAllowedToView");
        }

        if (clubId != null && !adminClubIds.contains(clubId)) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }

        List<UUID> target = (clubId != null) ? List.of(clubId) : adminClubIds;
        return bookingDao.countByClubIds(target, bookingId, status);
    }


    @Override
    public void delete(UUID bookingId) {
        bookingDao.delete(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchPaginated(UUID userId,
                                         UUID clubId,
                                         UUID bookingId,
                                         Status status,
                                         String sort,
                                         int limit,
                                         int offset) {
        return bookingDao.searchPaginated(userId, clubId, bookingId, status, sort, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearch(UUID userId,
                           UUID clubId,
                           UUID bookingId,
                           Status status) {
        return bookingDao.countSearch(userId, clubId, bookingId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> findAllPaginated(int limit, int offset) {
        return bookingDao.findAllPaginated(limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countAll() {
        return bookingDao.countAll();
    }

    @Override
    public Booking reserveTableForClub(UUID clubId, UUID tableId, LocalDate date, String username) {
        var tableOpt = tableService.findById(tableId);
        if (tableOpt.isEmpty()) throw new TableNotFoundException(tableId);
        var table = tableOpt.get();

        if (!clubId.equals(table.getClubId())) {
            throw new BadRequestException("El club no existe");
        }

        if (!ALLOWED_DAYS.contains(date.getDayOfWeek())) {
            throw new InvalidDateRangeException("Solo se puede reservar para jueves, viernes o sábado.");
        }

        OffsetDateTime start = date.atTime(23, 59).atZone(ZONE_ID).toOffsetDateTime();
        OffsetDateTime end   = date.plusDays(1).atTime(6, 0).atZone(ZONE_ID).toOffsetDateTime();

        var libres = tableService.findAvailableByClubAndRange(clubId, start, end);
        boolean disponible = libres.stream().anyMatch(t -> t.getTableId().equals(tableId));
        if (!disponible) {
            throw new BadRequestException("La mesa no está disponible en el rango indicado.");
        }

        var user = Optional.ofNullable(userService.findByUserName(username))
                .orElseGet(() -> userService.findByEmail(username));
        if (user == null) {
            throw new UserNotFoundException("No se encontró el usuario: " + username);
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BadRequestException("El usuario debe tener un email válido para recibir notificaciones");
        }

        BigDecimal total = table.getPrice() != null ? table.getPrice() : BigDecimal.ZERO;

        Booking booking = create(user, tableId, start, end, total);

        var club = clubService.getClubById(clubId).orElseThrow(IllegalArgumentException::new);
        var admins = clubService.getAdminsByClub(clubId);
        var owner = admins.isEmpty() ? null : admins.get(0);

        Locale locale = new Locale("es", "AR");
        try {
            emailService.notifyTableReservedToUser(booking, club, table, locale);
            if (owner != null && owner.getEmail() != null && !owner.getEmail().isBlank()) {
                emailService.notifyTableReservedToOwner(booking, club, table, owner, locale);
            }
        } catch (Exception e) {
            LOGGER.error("Error enviando emails de reserva para booking {}", booking.getBookingId(), e);
        }

        return booking;
    }

    private void maybeFreeTable(ClubTable table) {
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

    private void validateStatusTransition(Status from, Status to) {
        if (from == to) return;

        // Máquina de estados sólo para Booking (PENDIENTE/CONFIRMADA/CANCELADA)
        switch (from) {
            case PENDIENTE -> {
                if (!(to == Status.CONFIRMADA || to == Status.CANCELADA)) {
                    throw new InvalidStatusTransitionException(from, to);
                }
            }
            case CONFIRMADA -> {
                if (!(to == Status.CANCELADA)) {
                    throw new InvalidStatusTransitionException(from, to);
                }
            }
            case CANCELADA -> {
                throw new InvalidStatusTransitionException(from, to);
            }
            default -> throw new InvalidStatusTransitionException(from, to);
        }
    }

    private void validateDateRange(OffsetDateTime start, OffsetDateTime end) {
        if (!start.isBefore(end)) {
            throw new InvalidDateRangeException("Rango inválido: " + start + " - " + end);
        }
    }

    private ClubTable ensureTableExists(UUID tableId) {
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
                        (b.getStatus() == Status.PENDIENTE || b.getStatus() == Status.CONFIRMADA) &&
                                b.getBookingStart().isBefore(end) &&
                                b.getBookingEnd().isAfter(start)
                );

        if (overlaps) {
            throw new TableAlreadyBookedException();
        }
    }
}
