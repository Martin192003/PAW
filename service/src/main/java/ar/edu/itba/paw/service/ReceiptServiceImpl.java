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

    private final ReceiptDao dao;

    public ReceiptServiceImpl(ReceiptDao receiptRepository, BookingService bookingService, ReceiptDao dao) {
        this.receiptDao = receiptRepository;
        this.bookingService = bookingService;
        this.dao = dao; }

    @Override
    @Transactional
    public UUID upload(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("Empty image");
        if (data.length > MAX_BYTES) throw new IllegalArgumentException("Image too large");
        if (contentType == null || !ALLOWED.contains(contentType)) throw new IllegalArgumentException("Invalid type");
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("Invalid filename");
        return dao.create(bookingId, uploadedBy, filename, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> findByBooking(UUID bookingId) { return dao.findByBooking(bookingId); }

    @Override
    @Transactional(readOnly = true)
    public Optional<Receipt> getWithId(UUID receiptId) { return dao.findByIdWithData(receiptId); }

    @Override
    @Transactional
    public boolean deleteByBooking(UUID bookingId) { return dao.deleteByBooking(bookingId); }

    @Override
    public void uploadForBooking(UUID bookingId, UUID userId,
                                 String filename, String contentType, byte[] data) {

        // 1) Entrada básica
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

        // 2) Dominio: booking existe y es del usuario
        var bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new BookingNotFoundException("booking.notfound");
        }
        var booking = bookingOpt.get();
        if (booking.getUser() == null || !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("receipt.upload.notAllowed");
        }

         if (booking.getStatus() == Status.CANCELADA) throw new InvalidReceiptException("receipt.for.cancelled");

        // 3) Regla: un recibo por reserva (duplicado)
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

        Optional<Receipt> saved = receiptDao.findById(newReceiptId);

        // (Opcional) enviar notificación por email aquí si tu dominio lo requiere
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

}