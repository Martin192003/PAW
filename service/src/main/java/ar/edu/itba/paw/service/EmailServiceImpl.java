package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.BadRequestException;
import ar.edu.itba.paw.persistence.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class EmailServiceImpl implements EmailService {

    private static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String defaultFrom;
    private UserDao userDao;


    @Value("${local.base.url}")
    private String baseUrl;

    @Autowired
    public void setUserDao(final UserDao userDao) {
        this.userDao = userDao;
    }
    @Autowired
    private MessageSource messageSource;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine, UserDao userDao) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.userDao = userDao;
        String from = System.getenv("SMTP_USER");
        if ((from == null || from.isBlank()) && mailSender instanceof JavaMailSenderImpl) {
            from = ((JavaMailSenderImpl) mailSender).getUsername();
        }
        if (from == null || from.isBlank()) {
            from = "no-reply@local";
        }
        this.defaultFrom = from;
        LOGGER.info("MailService defaultFrom={}", this.defaultFrom);
    }

    private void sendTemplatedEmail(String to, String subject, String templateName,
                                    Map<String, Object> variables, String ctaText, String ctaUrl) {
        sendTemplatedEmail(to, subject, templateName, variables, ctaText, ctaUrl, null, null, null);
    }

    private void sendTemplatedEmail(String to, String subject, String templateName,
                                    Map<String, Object> variables, String ctaText, String ctaUrl,
                                    String attachmentName, byte[] attachmentData, String attachmentContentType) {
        try {
            Locale userLocale = LocaleContextHolder.getLocale();
            Context context = new Context(userLocale);

            Map<String, Object> mutableVariables = new HashMap<>(variables);

            if (ctaText != null) {
                mutableVariables.put("ctaText", ctaText);
            }
            if (ctaUrl != null) {
                mutableVariables.put("ctaUrl", ctaUrl);
            }

            context.setVariables(mutableVariables);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            // Agregar adjunto si está presente
            if (attachmentName != null && attachmentData != null && attachmentData.length > 0) {
                String mimeType = attachmentContentType != null ? attachmentContentType : "application/octet-stream";
                helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachmentData), mimeType);
            }

            mailSender.send(message);

            LOGGER.info("Templated email sent successfully to: {}", to);
        } catch (Exception e) {
            LOGGER.error("Failed to send templated email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async
    public boolean sendContact(String to, String clubName, String fromEmail, String name, String phone, String message) {
        try {
            Locale userLocale = LocaleContextHolder.getLocale();

            Map<String, Object> clubModel = Map.of(
                    "clubName", clubName,
                    "name", name,
                    "fromEmail", fromEmail,
                    "phone", phone,
                    "message", message,
                    "submittedAt", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "brandName", "BlingBling",
                    "brandFooter", "© 2025 BlingBling. Sistema de contacto."
            );

            sendTemplatedEmail(
                    to,
                    "Nueva consulta de contacto - " + clubName,
                    "booking-inquiry-club",
                    clubModel,
                    "Ver panel de mensajes",
                    abs("/clubs/messages")
            );

            Map<String, Object> userModel = Map.of(
                    "clubName", clubName,
                    "name", name,
                    "submittedAt", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "brandName", "BlingBling",
                    "brandFooter", "© 2025 BlingBling. Tu mensaje fue enviado correctamente."
            );

            sendTemplatedEmail(
                    fromEmail,
                    "Confirmación de contacto - " + clubName,
                    "booking-inquiry-user",
                    userModel,
                    "Ver clubs disponibles",
                    abs("/clubs")
            );

            LOGGER.info("SMTP contact emails sent OK to {} and {}", to, fromEmail);
            return true;
        } catch (Exception e) {
            LOGGER.error("SMTP contact emails FAILED: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async
    public void sendClubCreationConfirmation(String to, String clubName, UUID clubId) {
        Map<String, Object> model = Map.of(
                "clubName", clubName,
                "createdAt", ZonedDateTime.now(),
                "clubId", clubId,
                "brandName", "BlingBling",
                "brandFooter", "© 2025 BlingBling. Panel de administración."
        );

        sendTemplatedEmail(
                to,
                messageSource.getMessage("email.club.creation.title", null, LocaleContextHolder.getLocale()) + " - " + clubName,
                "club-creation-confirmation",
                model,
                "Gestionar mi club",
                abs("/clubs/handle?clubId=" + clubId)
        );
    }

    @Override
    @Async
    public void sendUserRegistrationConfirmation(String to, String username, String verificationToken) {
        String verificationUrl = abs("/verify-account?token=" + verificationToken);

        Map<String, Object> model = Map.of(
                "username", username,
                "token", verificationToken,
                "createdAt", ZonedDateTime.now(),
                "verificationUrl", verificationUrl,
                "brandName", "BlingBling",
                "brandFooter", "© 2025 BlingBling. Bienvenido a nuestra plataforma."
        );

        sendTemplatedEmail(
                to,
                messageSource.getMessage("email.registration.title.header", null, LocaleContextHolder.getLocale()) + " - " + username,
                "user-registration-confirmation",
                model,
                "Verificar mi cuenta",
                verificationUrl
        );
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        Map<String, Object> model = Map.of(
                "username", username,
                "requestedAt", ZonedDateTime.now(),
                "resetUrl", abs("/reset-password"),
                "token", resetToken,
                "brandName", "BlingBling",
                "brandFooter", "© 2025 BlingBling. Recuperación de contraseña."
        );

        String subject = messageSource.getMessage("email.password.reset.subject", null, LocaleContextHolder.getLocale()) +
                " - " + messageSource.getMessage("app.title", null, LocaleContextHolder.getLocale());

        sendTemplatedEmail(
                toEmail,
                subject,
                "password-reset-email",
                model,
                "Restablecer contraseña",
                abs("/reset-password")
        );
    }


    @Override
    @Async
    public void notifyTableReservedToUser(Booking booking, Club club, Table table, Locale locale) {
        Objects.requireNonNull(booking, "booking cannot be null");
        Objects.requireNonNull(booking.getUser(), "booking.user cannot be null");
        Objects.requireNonNull(club, "club cannot be null");
        Objects.requireNonNull(table, "table cannot be null");

        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BadRequestException("User email is required for notification");
        }

        LocalDate selectedDate = booking.getBookingStart().atZoneSameInstant(ZONE_ID).toLocalDate();
        sendTemplatedEmail(
                userEmail,
                "Reserva confirmada - " + club.getClubName(),
                "table-reserved-user",
                Map.of(
                        "clubName", club.getClubName(),
                        "tableNumber", table.getTableNumber(),
                        "capacity", table.getCapacity(),
                        "date", selectedDate.toString(),
                        "from", "23:59",
                        "to", "06:00"
                ),
                "Ver reserva",  abs("/booking?bookingId=" + booking.getBookingId())
        );
    }

    @Override
    @Async
    public void notifyTableReservedToOwner(Booking booking, Club club, Table table, User owner, Locale locale) {
        if (!isLikelyEmail(owner.getEmail())) {
            throw new BadRequestException("Email del dueño inválido: " + owner);
        }

        Objects.requireNonNull(booking, "booking es null");
        Objects.requireNonNull(booking.getUser(), "booking.user es null");
        Objects.requireNonNull(club, "club es null");
        Objects.requireNonNull(table, "table es null");

        // Campos que suelen ser nulos en datos incompletos:
        final String clubName   = Objects.requireNonNullElse(club.getClubName(), "Club");
        final String tableNum   = String.valueOf(table.getTableNumber());
        final Integer capacity  = table.getCapacity();
        final String username   = Objects.requireNonNullElse(booking.getUser().getUsername(), "usuario");
        final String userEmail  = Objects.requireNonNullElse(booking.getUser().getEmail(), "no-reply@example.test");

        final LocalDate date = Objects.requireNonNull(
                booking.getBookingStart(), "booking.bookingStart es null"
        ).atZoneSameInstant(ZONE_ID).toLocalDate();

        final DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        final String dateTimePretty = df.format(date) + " 23:59–06:00";
        final String ctaUrl = abs("/admin/clubs/bookings?userId=" + owner.getId());
        Map<String, Object> ownerModel = new HashMap<>();
        ownerModel.put("clubName", clubName);
        ownerModel.put("tableNumber", tableNum);
        ownerModel.put("capacity", capacity);
        ownerModel.put("dateTime", dateTimePretty);
        ownerModel.put("username", username);
        ownerModel.put("userEmail", userEmail);
        ownerModel.put("ctaUrl", ctaUrl);
        ownerModel.put("ctaText", "Abrir panel");
        ownerModel.put("brandName", "BlingBling");
        ownerModel.put("brandFooter", "© 2025 BlingBling.");
        ownerModel.put("sentAt", OffsetDateTime.now(ZONE_ID));

        sendTemplatedEmail(
                owner.getEmail(),
                "Nueva reserva de mesa - " + clubName,
                "table-reserved-owner",
                ownerModel,
                "Abrir panel", ctaUrl
        );
    }

    @Override
    @Async
    public void notifyBookingCancelledToUser(Booking booking, Club club, Table table, Locale locale) {
        Objects.requireNonNull(booking, "booking cannot be null");
        Objects.requireNonNull(booking.getUser(), "booking.user cannot be null");
        Objects.requireNonNull(club, "club cannot be null");
        Objects.requireNonNull(table, "table cannot be null");

        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BadRequestException("User email is required for cancellation notification");
        }

        LocalDate selectedDate = booking.getBookingStart().atZoneSameInstant(ZONE_ID).toLocalDate();

        sendTemplatedEmail(
                userEmail,
                "Reserva cancelada - " + club.getClubName(),
                "booking-cancelled-user",
                Map.of(
                        "clubName", club.getClubName(),
                        "tableNumber", table.getTableNumber(),
                        "capacity", table.getCapacity(),
                        "date", selectedDate.toString(),
                        "from", "23:59",
                        "to", "06:00",
                        "brandName", "BlingBling",
                        "brandFooter", "© 2025 BlingBling."
                ),
                "Ver otros clubs",
                abs("/clubs")
        );
    }

    @Override
    @Async
    public void notifyBookingCancelledToOwner(Booking booking, Club club, Table table, User owner, Locale locale) {
        if (!isLikelyEmail(owner.getEmail())) {
            throw new BadRequestException("Email del dueño inválido: " + owner);
        }

        Objects.requireNonNull(booking, "booking es null");
        Objects.requireNonNull(club, "club es null");
        Objects.requireNonNull(table, "table es null");

        final String clubName = Objects.requireNonNullElse(club.getClubName(), "Club");
        final String tableNum = String.valueOf(table.getTableNumber());
        final Integer capacity = table.getCapacity();

        final LocalDate date = Objects.requireNonNull(
                booking.getBookingStart(), "booking.bookingStart es null"
        ).atZoneSameInstant(ZONE_ID).toLocalDate();

        final DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        final String dateTimePretty = df.format(date) + " 23:59–06:00";

        Map<String, Object> ownerModel = Map.of(
                "clubName", clubName,
                "tableNumber", tableNum,
                "capacity", capacity,
                "dateTime", dateTimePretty,
                "brandName", "BlingBling",
                "brandFooter", "© 2025 BlingBling.",
                "sentAt", OffsetDateTime.now(ZONE_ID)
        );

        sendTemplatedEmail(
                owner.getEmail(),
                "Reserva cancelada - " + clubName,
                "booking-cancelled-owner",
                ownerModel,
                "Ver panel",
                abs("/admin/clubs/bookings?userId=" + owner.getId())
        );
    }

    @Override
    @Async
    public void notifyReceiptUploaded(Booking booking, Club club, Table table, User owner, Receipt receipt, Locale locale) {
        if (!isLikelyEmail(owner.getEmail())) {
            throw new BadRequestException("Email del dueño inválido: " + owner);
        }

        Objects.requireNonNull(booking, "booking es null");
        Objects.requireNonNull(club, "club es null");
        Objects.requireNonNull(table, "table es null");
        Objects.requireNonNull(receipt, "receipt es null");

        final String clubName = Objects.requireNonNullElse(club.getClubName(), "Club");
        final String tableNum = String.valueOf(table.getTableNumber());
        final String username = Objects.requireNonNullElse(booking.getUser().getUsername(), "usuario");
        final String userEmail = Objects.requireNonNullElse(booking.getUser().getEmail(), "no-reply@example.test");

        final LocalDate date = Objects.requireNonNull(
                booking.getBookingStart(), "booking.bookingStart es null"
        ).atZoneSameInstant(ZONE_ID).toLocalDate();

        final DateTimeFormatter df = DateTimeFormatter.ofPattern("EEE dd/MM", locale);
        final String dateTimePretty = df.format(date) + " 23:59–06:00";

        Map<String, Object> ownerModel = new HashMap<>();
        ownerModel.put("clubName", clubName);
        ownerModel.put("tableNumber", tableNum);
        ownerModel.put("username", username);
        ownerModel.put("userEmail", userEmail);
        ownerModel.put("dateTime", dateTimePretty);
        ownerModel.put("receiptFilename", receipt.getFilename());
        ownerModel.put("receiptUploadedAt", OffsetDateTime.now(ZONE_ID).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        ownerModel.put("bookingId", booking.getBookingId());
        ownerModel.put("brandName", "BlingBling");
        ownerModel.put("brandFooter", "© 2025 BlingBling. Recibo de pago cargado.");

        sendTemplatedEmail(
                owner.getEmail(),
                "Recibo cargado - " + clubName + " - Mesa " + tableNum,
                "receipt-uploaded-notification",
                ownerModel,
                "Ver recibo",
                abs("/admin/booking?bookingId=" + booking.getBookingId()),
                receipt.getFilename(),
                receipt.getData(),
                receipt.getContentType()
        );
    }

    private String abs(String path) {
        String b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return path.startsWith("/") ? b + path : b + "/" + path;
    }

    private boolean isLikelyEmail(String s) {
        return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

}