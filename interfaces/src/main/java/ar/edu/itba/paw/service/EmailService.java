package ar.edu.itba.paw.service;

import java.util.Locale;
import java.util.Map;
import ar.edu.itba.paw.model.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    void sendContact(String to, String clubName, String fromEmail, String name, String phone, String message);
    void notifyTableReservedToOwner(Booking booking, Club club,  ClubTable table, User owner, Locale locale);
    void notifyTableReservedToUser(Booking booking, Club club,  ClubTable table, Locale locale);
    void sendClubCreationConfirmation(String to, String clubName, UUID clubId);
    void sendUserRegistrationConfirmation(String to, String username, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String username, String resetToken);
    void notifyBookingCancelledToUser(Booking booking, Club club,  ClubTable table, Locale locale);
    void notifyBookingCancelledToOwner(Booking booking, Club club,  ClubTable table, User ownerEmail, Locale locale);

}

