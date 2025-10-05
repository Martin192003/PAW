package ar.edu.itba.paw.service;

import java.util.Locale;
import java.util.Map;
import ar.edu.itba.paw.model.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    boolean sendContact(String to, String clubName, String fromEmail, String name, String phone, String message);
    void sendBookingConfirmationToUser(User user, Club club, Table table, Booking booking);
    void sendBookingNotificationToOwner(Club club, Table table, Booking booking);
    void notifyTableReservedToOwner(Booking booking, Club club, Table table, String ownerEmail, Locale locale);
    void notifyTableReservedToUser(Booking booking, Club club, Table table, Locale locale);
    void sendClubCreationConfirmation(String to, String clubName, UUID clubId);
    void sendUserRegistrationConfirmation(String to, String username, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String username, String resetToken);

}

