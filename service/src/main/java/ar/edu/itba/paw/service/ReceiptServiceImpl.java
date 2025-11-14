package ar.edu.itba.paw.service;

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
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
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
    public Optional<Receipt> findByBooking(UUID bookingId) { return receiptDao.findByBooking(bookingId); }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> getWithId(UUID receiptId) { return receiptDao.findByIdWithData(receiptId); }

    @Override
    @Transactional
    public boolean deleteByBooking(UUID bookingId) { return receiptDao.deleteByBooking(bookingId); }

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

        String ct = (contentType == null || contentType.isBlank())
                ? "application/octet-stream" : contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED.contains(ct)) {
            throw new InvalidReceiptException("receipt.unsupportedType");
        }

        String safeName = (filename == null || filename.isBlank()) ? "receipt" : filename.trim();

        var bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new BookingNotFoundException("booking.notfound");
        }
        var booking = bookingOpt.get();
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("receipt.upload.notAllowed");
        }

        if (booking.getStatus() == Status.CANCELADA) throw new InvalidReceiptException("receipt.for.cancelled");

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

        LOGGER.info("Receipt created successfully with ID: {}", newReceiptId);

        // Obtener información adicional para el email
        try {
            var tableOpt = tableService.findById(booking.getTableId());
            if (tableOpt.isPresent()) {
                var table = tableOpt.get();
                LOGGER.info("Table found: {}", table.getTableId());

                var clubOpt = clubService.getClubById(table.getClubId());
                if (clubOpt.isPresent()) {
                    var club = clubOpt.get();
                    LOGGER.info("Club found: {}", club.getClubName());

                    var admins = clubService.getAdminsByClub(club.getClubId());
                    LOGGER.info("Found {} admins for club", admins.size());

                    if (!admins.isEmpty() && admins.get(0).getEmail() != null && !admins.get(0).getEmail().trim().isEmpty()) {
                        String adminEmail = admins.get(0).getEmail();
                        LOGGER.info("Sending receipt notification to admin: {}", adminEmail);

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

                        LOGGER.info("Receipt object created for email with data length: {}", data.length);

                        Locale locale = new Locale("es", "AR");
                        emailService.notifyReceiptUploaded(booking, club, table, admins.get(0), receiptForEmail, locale);

                        LOGGER.info("Receipt upload notification sent successfully to {}", adminEmail);
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
    public boolean deleteByBookingForUser(UUID bookingId, UUID userId) {

        // 1. Verificar existencia de la reserva
        var bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new BookingNotFoundException("booking.notfound");
        }
        var booking = bookingOpt.get();

        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("receipt.delete.notAllowed");
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
    @Transactional
    public Optional<Receipt> getReceiptForBooking(UUID bookingId) {
        return findByBooking(bookingId)
                .flatMap(meta -> getWithId(meta.getReceiptId())
                        .or(() -> Optional.of(meta)));
    }



}