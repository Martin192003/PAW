package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.persistence.ReceiptDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
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
    private final ReceiptDao receiptDao;

    public BookingServiceImpl(final BookingDao bookingDao,
                              final ClubTablesDao tablesDao,
                              final UserService userService,
                              final ClubService clubService,
                              final TableService tableService,
                              final ClubDao clubDao,
                              final EmailService emailService,
                              final ReceiptDao receiptDao) {
        this.bookingDao = bookingDao;
        this.tablesDao = tablesDao;
        this.userService = userService;
        this.clubService = clubService;
        this.tableService = tableService;
        this.clubDao = clubDao;
        this.emailService = emailService;
        this.receiptDao = receiptDao;
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


        if (status == Status.PENDIENTE || status == Status.CONFIRMADA) {
            ensureNoOverlap(tableId, bookingStart, bookingEnd, bookingId);
        }

        validateStatusTransition(existing.getStatus(), status);

        return bookingDao.update(bookingId, user, tableId, status, totalAmount, bookingStart, bookingEnd)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
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
                )
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        if (newStatus == Status.CANCELADA) {
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
    @Transactional
    public void cancel(UUID bookingId) {
        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        validateStatusTransition(existing.getStatus(), Status.CANCELADA);

        final ClubTable table = ensureTableExists(existing.getTableId());
        final Club club = clubDao.findById(table.getClubId()).orElseThrow();

        final User completeUser = userService.findById(existing.getUser().getId()).orElseThrow();
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

        Locale locale = completeUser.getLocale() != null
                ? Locale.forLanguageTag(completeUser.getLocale())
                : LocaleContextHolder.getLocale();
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
    @Transactional
    public void cancelByUser(UUID bookingId, UUID userId) {
        final Booking existing = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        if (!existing.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("El usuario no puede cancelar esta reserva");
        }

        validateStatusTransition(existing.getStatus(), Status.CANCELADA);

        final ClubTable table = ensureTableExists(existing.getTableId());
        final Club club = clubDao.findById(table.getClubId()).orElseThrow();

        final User completeUser = userService.findById(existing.getUser().getId()).orElseThrow();
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

        Locale locale = completeUser.getLocale() != null
                ? Locale.forLanguageTag(completeUser.getLocale())
                : LocaleContextHolder.getLocale();
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
    @Transactional
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
        if (receiptDao.findByBooking(bookingId).isEmpty()) {
            throw new InvalidReceiptException("receipt.required");
        }

        bookingDao.updateStatus(bookingId, Status.CONFIRMADA)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        try {
            final Booking updated = bookingDao.findById(bookingId).orElseThrow();
            final ClubTable table = ensureTableExists(updated.getTableId());
            final Club club = clubDao.findById(table.getClubId()).orElseThrow();
            final User completeUser = userService.findById(updated.getUser().getId())
                    .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + updated.getUser().getId()));
            Locale locale = completeUser.getLocale() != null
                    ? Locale.forLanguageTag(completeUser.getLocale())
                    : LocaleContextHolder.getLocale();
            emailService.notifyConfirmationEmailToUser(updated, club, table, locale);
        } catch (Exception e) {
            LOGGER.error("Error sending confirmation email for booking {}", bookingId, e);
        }
    }

    @Override
    @Transactional
    public void cancelForUserInClub(UUID clubId, UUID bookingId, String loginIdentifier) {
        final Booking booking = getForUserInClub(clubId, bookingId, loginIdentifier);
        cancelByUser(bookingId, booking.getUser().getId());
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
    public Booking getForUserInClub(UUID clubId, UUID bookingId, String loginIdentifier) {
        if (clubId == null) {
            throw new BadRequestException("clubId es requerido");
        }
        final User user = findUserByLoginIdentifier(loginIdentifier);
        final Booking booking = getForUser(bookingId, user.getId());
        final ClubTable table = ensureTableExists(booking.getTableId());
        if (!clubId.equals(table.getClubId())) {
            throw new BookingNotFoundException(bookingId.toString());
        }
        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getForUserOrAdminInClub(UUID clubId, UUID bookingId, String loginIdentifier) {
        if (clubId == null) {
            throw new BadRequestException("clubId es requerido");
        }
        final User user = findUserByLoginIdentifier(loginIdentifier);
        final Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

        final ClubTable table = ensureTableExists(booking.getTableId());
        if (!clubId.equals(table.getClubId())) {
            throw new BookingNotFoundException(bookingId.toString());
        }

        final boolean isOwner = booking.getUser() != null
                && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId());

        final List<UUID> adminClubIds = clubDao.findIdsByAdmin(user.getId());
        final boolean isClubAdmin = adminClubIds != null && adminClubIds.contains(clubId);

        if (!isOwner && !isClubAdmin) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }

        return booking;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchForAdminPaginated(UUID adminUserId,
                                                 UUID clubId,
                                                 String oldBookingId,
                                                 Status status,
                                                 String sort,
                                                 String oldBookingDate,
                                                 Boolean hasReceipt,
                                                 int limit,
                                                 int offset) {

        UUID bookingId = parseBookingId(oldBookingId);
        LocalDate bookingDate = parseBookingDate(oldBookingDate);
        List<UUID> adminClubIds = clubDao.findIdsByAdmin(adminUserId);
        if (adminClubIds == null || adminClubIds.isEmpty()) {
            return List.of();
        }

        if (clubId != null) {
            if (!adminClubIds.contains(clubId)) {
                return List.of();
            }
            adminClubIds = List.of(clubId);
        }

        return bookingDao.searchByClubIdsPaginated(adminClubIds, bookingId, status, sort, bookingDate, hasReceipt, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchForUserPaginated(String loginIdentifier, UUID clubId, String bookingId, Status status, String sort, String bookingDate, Boolean hasReceipt, int limit, int offset) {
        final User user = findUserByLoginIdentifier(loginIdentifier);
        return searchPaginated(user.getId(), clubId, bookingId, status, sort, bookingDate, hasReceipt, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearchForUser(String loginIdentifier, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt) {
        final User user = findUserByLoginIdentifier(loginIdentifier);
        return countSearch(user.getId(), clubId, bookingId, status, bookingDate, hasReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchManagedClubsPaginated(String loginIdentifier, UUID clubId, String bookingId, Status status, String sort, String bookingDate, Boolean hasReceipt, int limit, int offset) {
        final User user = findUserByLoginIdentifier(loginIdentifier);
        return searchForAdminPaginated(user.getId(), clubId, bookingId, status, sort, bookingDate, hasReceipt, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearchManagedClubs(String loginIdentifier, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt) {
        final User user = findUserByLoginIdentifier(loginIdentifier);
        return countSearchForAdmin(user.getId(), clubId, bookingId, status, bookingDate, hasReceipt);
    }

    @Override
    @Transactional(readOnly = true)
    public Booking getManagedClubBooking(String loginIdentifier, UUID bookingId) {
        final User user = findUserByLoginIdentifier(loginIdentifier);
        final Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        final UUID clubId = clubId(bookingId);
        final List<UUID> adminClubIds = clubDao.findIdsByAdmin(user.getId());
        if (adminClubIds == null || !adminClubIds.contains(clubId)) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }
        return booking;
    }

    @Override
    @Transactional
    public void confirmManagedClubBooking(String loginIdentifier, UUID bookingId) {
        getManagedClubBooking(loginIdentifier, bookingId);
        confirm(bookingId);
    }

    @Override
    @Transactional
    public void cancelManagedClubBooking(String loginIdentifier, UUID bookingId) {
        getManagedClubBooking(loginIdentifier, bookingId);
        cancel(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> searchPaginated(UUID userId,
                                         UUID clubId,
                                         String oldBookingId,
                                         Status status,
                                         String sort,
                                         String oldBookingDate,
                                         Boolean hasReceipt,
                                         int limit,
                                         int offset) {
        UUID bookingId = parseBookingId(oldBookingId);
        LocalDate bookingDate = parseBookingDate(oldBookingDate);
        return bookingDao.searchPaginated(userId, clubId, bookingId, status, sort, bookingDate, hasReceipt, limit, offset);
    }


    @Override
    @Transactional(readOnly = true)
    public int countSearch(UUID userId,
                           UUID clubId,
                           String oldBookingId,
                           Status status,
                           String oldBookingDate,
                           Boolean hasReceipt) {
        UUID bookingId = parseBookingId(oldBookingId);
        LocalDate bookingDate = parseBookingDate(oldBookingDate);
        return bookingDao.countSearch(userId, clubId, bookingId, status, bookingDate, hasReceipt);
    }



    @Override
    @Transactional(readOnly = true)
    public int countSearchForAdmin(UUID adminUserId, UUID clubId, String oldBookingId, Status status, String oldBookingDate, Boolean hasReceipt) {

        UUID bookingId = parseBookingId(oldBookingId);
        LocalDate bookingDate = parseBookingDate(oldBookingDate);
        List<UUID> adminClubIds = clubDao.findIdsByAdmin(adminUserId);
        if (adminClubIds == null || adminClubIds.isEmpty()) {
            throw  new UnauthorizedException("booking.notAllowedToView");
        }

        if (clubId != null && !adminClubIds.contains(clubId)) {
            throw new UnauthorizedException("booking.notAllowedToView");
        }

        List<UUID> target = (clubId != null) ? List.of(clubId) : adminClubIds;
        return bookingDao.countByClubIds(target, bookingId, status, bookingDate, hasReceipt);
    }

    @Override
    public String getBookingDateStart(UUID bookingId) {
        Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        return formatDateEsEn(booking.getBookingStart().atZoneSameInstant(ZoneId.systemDefault()));
    }

    @Override
    public String getBookingDateEnd(UUID bookingId) {
        Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        return formatDateEsEn(booking.getBookingStart().atZoneSameInstant(ZoneId.systemDefault()));
    }

    @Override
    @Transactional
    public String getDateStart(UUID bookingId, UUID userId) {
        Booking booking = getForUser(bookingId, userId);
        return formatDateEsEn(booking.getBookingStart().atZoneSameInstant(ZoneId.systemDefault()));
    }

    @Override
    @Transactional
    public String getDateEnd(UUID bookingId, UUID userId) {
        Booking booking = getForUser(bookingId, userId);
        return formatDateEsEn(booking.getBookingEnd().atZoneSameInstant(ZoneId.systemDefault()));
    }


    @Override
    @Transactional
    public void delete(UUID bookingId) {
        bookingDao.delete(bookingId);
    }


    @Override
    @Transactional
    public Booking createForClub(UUID clubId, UUID tableId, LocalDate date, String loginIdentifier) {
        if (clubId == null) {
            throw new BadRequestException("clubId es requerido");
        }
        if (tableId == null) {
            throw new BadRequestException("tableId es requerido");
        }
        if (date == null) {
            throw new BadRequestException("date es requerido");
        }
        if (loginIdentifier == null || loginIdentifier.isBlank()) {
            throw new UnauthorizedException("booking.authenticationRequired");
        }

        var tableOpt = tableService.findById(tableId);
        if (tableOpt.isEmpty()) throw new TableNotFoundException(tableId);
        var table = tableOpt.get();

        if (!clubId.equals(table.getClubId())) {
            throw new BadRequestException("El club no existe");
        }

        OffsetDateTime start = date.atTime(23, 59).atZone(ZONE_ID).toOffsetDateTime();
        OffsetDateTime end   = date.plusDays(1).atTime(6, 0).atZone(ZONE_ID).toOffsetDateTime();

        var libres = tableService.findAvailableByClubAndRange(clubId, start, end);
        boolean disponible = libres.stream().anyMatch(t -> t.getTableId().equals(tableId));
        if (!disponible) {
            throw new BadRequestException("La mesa no está disponible en el rango indicado.");
        }

        var user = findUserByLoginIdentifier(loginIdentifier);
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BadRequestException("El usuario debe tener un email válido para recibir notificaciones");
        }

        BigDecimal total = table.getPrice() != null ? table.getPrice() : BigDecimal.ZERO;

        Booking booking = create(user, tableId, start, end, total);

        var club = clubService.getClubById(clubId).orElseThrow(IllegalArgumentException::new);
        var admins = clubService.getAdminsByClub(clubId);
        var owner = admins.isEmpty() ? null : admins.get(0);

        Locale locale = user.getLocale() != null
                ? Locale.forLanguageTag(user.getLocale())
                : LocaleContextHolder.getLocale();
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

    private User findUserByLoginIdentifier(String loginIdentifier) {
        if (loginIdentifier == null || loginIdentifier.isBlank()) {
            throw new UnauthorizedException("booking.authenticationRequired");
        }

        User user = userService.findByUserName(loginIdentifier);
        if (user == null) {
            user = userService.findByEmail(loginIdentifier);
        }
        if (user == null) {
            throw new UserNotFoundException("No se encontró el usuario: " + loginIdentifier);
        }
        return user;
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


    @Override
    @Transactional(readOnly = true)
    public int getTotalPages(UUID userId, UUID clubId, String bookingId, Status status, String bookingDate, Boolean hasReceipt, int size) {
        int totalBookings = countSearch(userId, clubId, bookingId, status, bookingDate, hasReceipt);
        return (int) Math.ceil((double) totalBookings / size);

    }

    @Override
    @Transactional(readOnly = true)
    public User getBookingUser(UUID bookingId, UUID userId) {
        Booking booking = getForUser(bookingId, userId);
        return userService.findById(booking.getUser().getId()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ClubTable getBookingTable(UUID bookingId, UUID userId) {
        Booking booking = getForUser(bookingId, userId);
        return tableService.findById(booking.getTableId()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Club getBookingClub(UUID bookingId, UUID userId) {
        ClubTable table = getBookingTable(bookingId, userId);
        if (table == null) {
            return null;
        }
        return clubService.getClubById(table.getClubId()).orElse(null);
    }

    public UUID parseBookingId(String bookingId) {
        if (bookingId == null || bookingId.isBlank()) return null;
        try {
            return UUID.fromString(bookingId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public LocalDate parseBookingDate(String bookingDate) {
        if (bookingDate == null || bookingDate.isBlank()) return null;
        try {
            return LocalDate.parse(bookingDate);
        } catch (Exception e) {
            return null;
        }
    }

    public String buildQueryParams(Map<String, String> filters) {
        return filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    public Map<UUID, Date> mapBookingStartDates(List<Booking> bookings) {
        Map<UUID, Date> bookingStartDates = new HashMap<>();
        for (Booking booking : bookings) {
            if (booking.getBookingStart() != null) {
                bookingStartDates.put(booking.getBookingId(), Date.from(booking.getBookingStart().toInstant()));
            }
        }
        return bookingStartDates;
    }

    private void validateStatusTransition(Status from, Status to) {
        if (from == to) return;

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

    private static String formatDateEsEn(ZonedDateTime zdt) {
        var date = zdt.toLocalDate();
        if (date == null) return "";
        Locale locale = LocaleContextHolder.getLocale();
        boolean isEs = "es".equalsIgnoreCase(locale.getLanguage());

        Month m = date.getMonth();
        String monthName = m.getDisplayName(TextStyle.FULL, locale);
        if (isEs) {
            monthName = capitalize(monthName, locale);
        }

        String connector = isEs ? " de " : " of ";
        return date.getDayOfMonth() + connector + monthName + ", " + date.getYear();
    }

    private static String capitalize(String s, Locale locale) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(locale) + s.substring(1);
    }

    @Override
    public UUID clubId(UUID bookingId) {
        Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));
        ClubTable table = tableService.findById(booking.getTableId())
                .orElseThrow(() -> new TableNotFoundException(booking.getTableId()));
        return table.getClubId();
    }
}
