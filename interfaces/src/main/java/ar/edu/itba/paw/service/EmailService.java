package ar.edu.itba.paw.service;

import java.util.Map;
import ar.edu.itba.paw.model.*;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    CompletableFuture<Void> sendTemplatedEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> model,
            String ctaText,
            String ctaUrl
    );
    boolean sendContact(String to, String clubName, String fromEmail, String name, String phone, String message);
    void sendBookingConfirmationToUser(User user, Club club, Table table, Booking booking);
    void sendBookingNotificationToOwner(Club club, Table table, Booking booking);
    void sendClubCreationConfirmation(String to, String clubName);
    void sendUserRegistrationConfirmation(String to, String username);

}

