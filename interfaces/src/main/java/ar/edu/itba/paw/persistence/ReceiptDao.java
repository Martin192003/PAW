package ar.edu.itba.paw.persistence;

import java.util.Optional;
import java.util.UUID;
import ar.edu.itba.paw.model.Receipt;

public interface ReceiptDao {
    UUID create(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data);
    Optional<Receipt> findByBooking(UUID bookingId);
    Optional<Receipt> findById(UUID receiptId);
    Optional<Receipt> findByIdWithData(UUID receiptId);
    boolean deleteByBooking(UUID bookingId);
    boolean delete(UUID receiptId);

}
