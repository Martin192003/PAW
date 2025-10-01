package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Receipt;
import ar.edu.itba.paw.persistence.ReceiptDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ReceiptServiceImpl implements ReceiptService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final ReceiptDao dao;

    public ReceiptServiceImpl(ReceiptDao dao) { this.dao = dao; }

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
}