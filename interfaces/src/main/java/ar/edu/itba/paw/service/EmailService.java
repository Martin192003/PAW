package ar.edu.itba.paw.service;

import java.util.Locale;
import ar.edu.itba.paw.model.*;

import java.util.UUID;

public interface EmailService {

    void sendContact(String to, String clubName, String fromEmail, String name, String phone, String message);

    void notifyTableReservedToOwner(Booking booking, Club club, ClubTable table, User owner, Locale locale);

    void notifyTableReservedToUser(Booking booking, Club club, ClubTable table, Locale locale);

    void sendClubCreationConfirmation(String to, String clubName, UUID clubId);

    void sendUserRegistrationConfirmation(String to, String username, String verificationToken);

    void sendPasswordResetEmail(String toEmail, String username, String resetToken);

    void notifyBookingCancelledToUser(Booking booking, Club club, ClubTable table, Locale locale);

    void notifyBookingCancelledToOwner(Booking booking, Club club, ClubTable table, User ownerEmail, Locale locale);

    void notifyReceiptUploaded(Booking booking, Club club, ClubTable table, User owner, Receipt receipt, Locale locale);

    void notifyConfirmationEmailToUser(Booking booking, Club club, ClubTable table, Locale locale);

    void sendOwnerReviewAlert(Club club, Review review, Locale locale);

}