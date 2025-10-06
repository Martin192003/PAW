package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.model.exceptions.*;
import org.springframework.security.access.AccessDeniedException;
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

    private final BookingDao bookingDao;
    private final ClubTablesDao tablesDao;
    private final UserService userService;
    private final ClubService clubService;
    private final TableService tableService;
    private final ClubDao clubDao;
    private final EmailServiceImpl emailService;
    private static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Set<DayOfWeek> ALLOWED_DAYS = EnumSet.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    public BookingServiceImpl(final BookingDao bookingDao, final ClubTablesDao tablesDao, UserService userService, ClubService clubService, TableService tableService, final ClubDao clubDao, final EmailServiceImpl emailService) {
        this.bookingDao = bookingDao;
        this.tablesDao = tablesDao;
        this.userService = userService;
        this.clubService = clubService;
        this.tableService = tableService;
        this.clubDao = clubDao;
        this.emailService = emailService;
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

        Club club = clubDao.findById(table.getClubId()).orElseThrow();
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

        return bookingDao.update(bookingId, user, tableId, status, totalAmount, bookingStart, bookingEnd).orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
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
        ).orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));


        if (newStatus == Status.CANCELADA) {
            try {
                final Table table = ensureTableExists(updated.getTableId());
                maybeFreeTable(table);
            } catch (TableNotFoundException ignored) {
            }
        }

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

        try {
            final Table table = ensureTableExists(existing.getTableId());
            maybeFreeTable(table);
        } catch (TableNotFoundException ignored) {
        }


        return true;
    }
    @Transactional
    @Override
    public void cancelByUser(UUID bookingId, UUID userId){
        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        if(!existing.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("El usuario no puede cancelar esta reserva");
        }
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
    }

    @Transactional
    @Override
    public void confirm(UUID bookingId) {
        Booking b = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("booking.notfound"));

        if (b.getStatus() == Status.CANCELADA) {
            throw new InvalidStatusTransitionException(b.getStatus(),Status.CANCELADA);
        }
        if (b.getStatus() == Status.CONFIRMADA) {
            return; // idempotencia
        }
        if (b.getStatus() != Status.PENDIENTE) {
            throw new InvalidStatusTransitionException(b.getStatus(),Status.PENDIENTE);
        }

        b.setStatus(Status.CONFIRMADA);
        bookingDao.updateStatus(bookingId,b.getStatus()).orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));


    }


    @Override
    @Transactional
    public boolean delete(UUID bookingId) {
        return bookingDao.delete(bookingId);
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



    @Transactional
    @Override
    public void reserveTableForClub(UUID clubId, UUID tableId, LocalDate date, String username) {
        // Mesa
        var tableOpt = tableService.findById(tableId);
        if (tableOpt.isEmpty()) throw new TableNotFoundException(tableId);
        var table = tableOpt.get();
        if (!clubId.equals(table.getClubId())) {
            throw new BadRequestException("El club no existe");
        }

        // Día permitido
        DayOfWeek dow = date.getDayOfWeek();
        if (!ALLOWED_DAYS.contains(dow)) {
            throw new InvalidDateRangeException("Solo se puede reservar para jueves, viernes o sábado.");
        }

        // Ventana 23:59–06:00 en es-AR
        OffsetDateTime start = date.atTime(23, 59).atZone(ZONE_ID).toOffsetDateTime();
        OffsetDateTime end   = date.plusDays(1).atTime(6, 0).atZone(ZONE_ID).toOffsetDateTime();

        // Disponibilidad
        var libres = tableService.findAvailableByClubAndRange(clubId, start, end);
        boolean disponible = libres.stream().anyMatch(t -> t.getTableId().equals(tableId));
        if (!disponible) throw new BadRequestException("La mesa no está disponible en el rango indicado.");

        // Usuario
        var user = userService.findByUserName(username);
        if (user == null) {
            user = userService.findByEmail(username);
        }
        if (user == null) throw new UserNotFoundException("No se encontró el usuario: " + username);

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BadRequestException("El usuario debe tener un email válido para recibir notificaciones");
        }

        // Precio
        BigDecimal total = table.getPrice() != null ? table.getPrice() : BigDecimal.ZERO;

        // Crear booking (el service debe re-validar disponibilidad en la transacción)
        Booking booking = create(user, tableId, start, end, total);


        // Datos de club y notificaciones
        var club = clubService.getClubById(clubId).orElseThrow(IllegalArgumentException::new);
        User ownerEmail = clubService.getAdminsByClub(clubId).getFirst();

        Locale locale = new Locale("es", "AR");
        emailService.notifyTableReservedToUser(booking, club, table, locale);
        emailService.notifyTableReservedToOwner(booking, club, table, ownerEmail, locale);

    }

    @Transactional(readOnly = true)
    @Override
    public Booking getForUser(UUID bookingId, UUID userId) {
        Booking b = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("booking.notfound"));
        if (b.getUser() == null || !b.getUser().getId().equals(userId)) {
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
                                                 int limit, int offset) {

        List<UUID> adminClubIds = clubDao.findIdsByAdmin(adminUserId);
        if (adminClubIds.isEmpty()) return List.of();

        if (clubId != null) {
            if (!adminClubIds.contains(clubId)) return List.of(); // o AccessDeniedException
            adminClubIds = List.of(clubId);
        }
        return bookingDao.searchByClubIdsPaginated(adminClubIds, bookingId, status, sort, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearchForAdmin(UUID adminUserId, UUID clubId, UUID bookingId, Status status) {
        List<UUID> adminClubIds = clubDao.findIdsByAdmin(adminUserId);
        if (adminClubIds.isEmpty()) return 0;
        if (clubId != null && !adminClubIds.contains(clubId)) return 0;
        List<UUID> target = (clubId != null) ? List.of(clubId) : adminClubIds;
        return bookingDao.countByClubIds(target, bookingId, status);
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
                        // only consider active bookings (pending or confirmed) when checking overlap
                        (b.getStatus() == Status.PENDIENTE || b.getStatus() == Status.CONFIRMADA) &&
                                b.getBookingStart().isBefore(end) &&
                                b.getBookingEnd().isAfter(start)
                );

        if (overlaps) throw new TableAlreadyBookedException();
    }





}
