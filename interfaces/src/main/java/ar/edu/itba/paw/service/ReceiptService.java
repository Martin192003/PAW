package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Receipt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptService {
    UUID upload(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data);
    Optional<Receipt> findByBooking(UUID bookingId);
    Optional<Receipt> getWithId(UUID receiptId);
    boolean deleteByBooking(UUID bookingId);
    void uploadForBooking(UUID bookingId, UUID userId, String filename, String contentType, byte[] data);
    boolean deleteByBookingForUser(UUID bookingId, UUID userId);
    Optional<Receipt> getReceiptForBooking(UUID bookingId);
    Map<UUID, Boolean> buildHasReceiptMap(List<Booking> bookings);

}