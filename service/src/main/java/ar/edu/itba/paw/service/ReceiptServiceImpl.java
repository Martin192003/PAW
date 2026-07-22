package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.exceptions.BookingNotFoundException;
import ar.edu.itba.paw.model.exceptions.InvalidReceiptException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.persistence.ReceiptDao;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> CONTENT_TYPE_ALIASES = Map.of(
            "image/jpg", "image/jpeg",
            "image/pjpeg", "image/jpeg",
            "image/x-png", "image/png"
    );
    private final ReceiptDao receiptDao;
    private final BookingService bookingService;

    private final TableService tableService;
    private final ClubService clubService;
    private final EmailService emailService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptServiceImpl.class);

    public ReceiptServiceImpl(ReceiptDao receiptDao, BookingService bookingService,
                              TableService tableService, ClubService clubService,
                              EmailService emailService) {
        this.receiptDao = receiptDao;
        this.bookingService = bookingService;
        this.tableService = tableService;
        this.clubService = clubService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public UUID upload(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("Empty image");
        if (data.length > MAX_BYTES) throw new IllegalArgumentException("Image too large");
        if (contentType == null || !ALLOWED.contains(contentType)) throw new IllegalArgumentException("Invalid type");
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("Invalid filename");
        return receiptDao.create(bookingId, uploadedBy, filename, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findByBooking(UUID bookingId) {
        return receiptDao.findByBooking(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> getWithId(UUID receiptId) {
        return receiptDao.findByIdWithData(receiptId);
    }

    @Override
    @Transactional
    public boolean deleteByBooking(UUID bookingId) {
        return receiptDao.deleteByBooking(bookingId);
    }

    @Override
    @Transactional
    public void uploadForBooking(UUID bookingId, UUID userId,
                                 String filename, String contentType, byte[] data) {

        if (data == null || data.length == 0) {
            throw new InvalidReceiptException("receipt.empty");
        }
        if (data.length > MAX_BYTES) {
            throw new InvalidReceiptException("receipt.tooLarge");
        }

        String safeName = (filename == null || filename.isBlank()) ? "receipt" : filename.trim();
        String ct = normalizeContentType(safeName, contentType);
        if (!ALLOWED.contains(ct)) {
            throw new InvalidReceiptException("receipt.unsupportedType");
        }

        var bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new BookingNotFoundException("booking.notfound");
        }
        var booking = bookingOpt.get();
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("receipt.upload.notAllowed");
        }

        if (booking.getStatus() == Status.CANCELADA) throw new InvalidReceiptException("receipt.for.cancelled");
        if (booking.getStatus() == Status.CONFIRMADA) throw new InvalidReceiptException("receipt.for.confirmed");

        if (receiptDao.findByBooking(bookingId).isPresent()) {
            throw new DuplicateKeyException("receipt.already.exists");
        }

        final UUID newReceiptId = receiptDao.create(
                bookingId,
                userId,
                safeName,
                ct,
                data
        );

        LOGGER.debug("Receipt created successfully with ID: {}", newReceiptId);

        try {
            var tableOpt = tableService.findById(booking.getTableId());
            if (tableOpt.isPresent()) {
                var table = tableOpt.get();
                LOGGER.debug("Table found: {}", table.getTableId());

                var clubOpt = clubService.getClubById(table.getClubId());
                if (clubOpt.isPresent()) {
                    var club = clubOpt.get();
                    LOGGER.debug("Club found: {}", club.getClubName());

                    var admins = clubService.getAdminsByClub(club.getClubId());
                    LOGGER.debug("Found {} admins for club", admins.size());

                    if (!admins.isEmpty() && admins.get(0).getEmail() != null && !admins.get(0).getEmail().trim().isEmpty()) {
                        String adminEmail = admins.get(0).getEmail();
                        LOGGER.debug("Sending receipt notification to admin: {}", adminEmail);

                        Receipt receiptForEmail = new Receipt(
                                newReceiptId,
                                bookingId,
                                userId,
                                null, // url
                                safeName,
                                ct,
                                (long) data.length, // sizeBytes
                                OffsetDateTime.now(),
                                data
                        );

                        LOGGER.debug("Receipt object created for email with data length: {}", data.length);

                        Locale locale = new Locale("es", "AR");
                        emailService.notifyReceiptUploaded(booking, club, table, admins.get(0), receiptForEmail, locale);

                        LOGGER.debug("Receipt upload notification sent successfully to {}", adminEmail);
                    } else {
                        LOGGER.warn("No valid admin email found for club {}", club.getClubId());
                    }
                } else {
                    LOGGER.warn("Club not found for table {}", table.getTableId());
                }
            } else {
                LOGGER.warn("Table not found for booking {}", bookingId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send receipt upload notification for booking {}", bookingId, e);
        }
    }

    @Override
    @Transactional
    public Receipt putForBookingInClub(UUID clubId, UUID bookingId, String loginIdentifier,
                                       String filename, String contentType, byte[] data) {

        final Booking booking = bookingService.getForUserOrAdminInClub(clubId, bookingId, loginIdentifier);
        if (booking.getStatus() == Status.CANCELADA) throw new InvalidReceiptException("receipt.for.cancelled");
        if (booking.getStatus() == Status.CONFIRMADA) throw new InvalidReceiptException("receipt.for.confirmed");

        receiptDao.findByBooking(bookingId)
                .ifPresent(receipt -> receiptDao.delete(receipt.getReceiptId()));

        uploadForBooking(bookingId, booking.getUser().getId(), filename, contentType, data);
        return getReceiptForBooking(bookingId)
                .orElseThrow(() -> new InvalidReceiptException("receipt.notfound"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> getForBookingInClub(UUID clubId, UUID bookingId, String loginIdentifier) {
        bookingService.getForUserOrAdminInClub(clubId, bookingId, loginIdentifier);
        return getReceiptForBooking(bookingId);
    }

    @Override
    @Transactional
    public boolean deleteByBookingInClub(UUID clubId, UUID bookingId, String loginIdentifier) {
        final Booking booking = bookingService.getForUserOrAdminInClub(clubId, bookingId, loginIdentifier);
        return deleteByBookingForUser(bookingId, booking.getUser().getId());
    }

    @Override
    @Transactional
    public boolean deleteByBookingForUser(UUID bookingId, UUID userId) {
        var bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new BookingNotFoundException("booking.notfound");
        }
        var booking = bookingOpt.get();

        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("receipt.delete.notAllowed");
        }
        if (booking.getStatus() == Status.CONFIRMADA) {
            throw new InvalidReceiptException("receipt.for.confirmed");
        }

        var receiptOpt = receiptDao.findByBooking(bookingId);
        if (receiptOpt.isEmpty()) {
            return false;
        }

        var receipt = receiptOpt.get();
        receiptDao.delete(receipt.getReceiptId());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> getReceiptForBooking(UUID bookingId) {
        return findByBooking(bookingId)
                .flatMap(meta -> getWithId(meta.getReceiptId())
                        .or(() -> Optional.of(meta)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Boolean> buildHasReceiptMap(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<UUID> bookingIds = bookings.stream()
                .map(Booking::getBookingId)
                .toList();
        final Set<UUID> withReceipt = receiptDao.findBookingIdsWithReceipt(bookingIds);

        Map<UUID, Boolean> map = new HashMap<>();
        for (UUID bookingId : bookingIds) {
            map.put(bookingId, withReceipt.contains(bookingId));
        }
        return map;
    }

    private String normalizeContentType(String filename, String contentType) {
        String ct = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        ct = CONTENT_TYPE_ALIASES.getOrDefault(ct, ct);

        if (ALLOWED.contains(ct)) {
            return ct;
        }

        String inferred = inferContentType(filename);
        if (inferred != null && (ct.isBlank() || "application/octet-stream".equals(ct))) {
            return inferred;
        }

        return ct.isBlank() ? "application/octet-stream" : ct;
    }

    private String inferContentType(String filename) {
        if (filename == null) {
            return null;
        }

        String lower = filename.trim().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return null;
    }
}
