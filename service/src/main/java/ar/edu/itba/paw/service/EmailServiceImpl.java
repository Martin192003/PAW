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
import ar.edu.itba.paw.persistence.BookingDao;
import java.lang.reflect.Method;

import javax.mail.internet.MimeMessage;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;


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
            Locale userLocale = userDao.findByEmail(to)
                    .map(user -> user.getLocale() != null ? Locale.forLanguageTag(user.getLocale()) : LocaleContextHolder.getLocale())
                    .orElse(LocaleContextHolder.getLocale());            Context context = new Context(userLocale);

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
    public void sendContact(String to, String clubName, String fromEmail, String name, String phone, String message) {
        /*try {
            Locale userLocale = userDao.findByEmail(to)
                    .map(user -> user.getLocale() != null ? Locale.forLanguageTag(user.getLocale()) : LocaleContextHolder.getLocale())
                    .orElse(LocaleContextHolder.getLocale());

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

            String subject = messageSource.getMessage("email.contact.confirmation.subject", null, userLocale) + " - " + clubName;

            sendTemplatedEmail(
                    fromEmail,
                    "Confirmación de contacto - " + clubName,
                    "booking-inquiry-user",
                    userModel,
                    "Ver clubs disponibles",
                    abs("/clubs")
            );

            LOGGER.info("SMTP contact emails sent OK to {} and {}", to, fromEmail);
        } catch (Exception e) {
            LOGGER.error("SMTP contact emails FAILED: {}", e.getMessage(), e);
        }*/
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

        Locale userLocale = userDao.findByEmail(to)
                .map(user -> user.getLocale() != null ? Locale.forLanguageTag(user.getLocale()) : LocaleContextHolder.getLocale())
                .orElse(LocaleContextHolder.getLocale());


        String subject = messageSource.getMessage("email.club.creation.title", null, userLocale) + " - " + clubName;

        sendTemplatedEmail(
                to,
                subject,
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

        Locale userLocale = userDao.findByEmail(to)
                .map(user -> user.getLocale() != null ? Locale.forLanguageTag(user.getLocale()) : LocaleContextHolder.getLocale())
                .orElse(LocaleContextHolder.getLocale());


        String subject = messageSource.getMessage("email.registration.title.header", null, userLocale) + " - " + username;

        sendTemplatedEmail(
                to,
                subject,
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

        Locale userLocale = userDao.findByEmail(toEmail)
                .map(user -> user.getLocale() != null ? Locale.forLanguageTag(user.getLocale()) : LocaleContextHolder.getLocale())
                .orElse(LocaleContextHolder.getLocale());

        String subject = messageSource.getMessage("email.password.reset.subject", null, LocaleContextHolder.getLocale()) +
                " - " + messageSource.getMessage("app.title", null, userLocale);

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
    public void notifyTableReservedToUser(Booking booking, Club club, ClubTable table, Locale locale) {
        Objects.requireNonNull(booking, "booking cannot be null");
        Objects.requireNonNull(booking.getUser(), "booking.user cannot be null");
        Objects.requireNonNull(club, "club cannot be null");
        Objects.requireNonNull(table, "table cannot be null");

        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BadRequestException("User email is required for notification");
        }

        LocalDate selectedDate = booking.getBookingStart().atZoneSameInstant(ZONE_ID).toLocalDate();


        String subject = messageSource.getMessage("email.booking.confirmed", new Object[] { club.getClubName() }, locale);

        sendTemplatedEmail(
                userEmail,
                subject,
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
    public void notifyTableReservedToOwner(Booking booking, Club club, ClubTable table, User owner, Locale locale) {
        if (!isLikelyEmail(owner.getEmail())) {
            throw new BadRequestException("Email del dueño inválido: " + owner);
        }

        Objects.requireNonNull(booking, "booking es null");
        Objects.requireNonNull(booking.getUser(), "booking.user es null");
        Objects.requireNonNull(club, "club es null");
        Objects.requireNonNull(table, "table es null");

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
        final String ctaUrl = abs("/admin/clubs/bookings?userId=" + booking.getBookingId());
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

        String subject = messageSource.getMessage("email.booking.new", new Object[] { club.getClubName() }, locale);


        sendTemplatedEmail(
                owner.getEmail(),
                subject,
                "table-reserved-owner",
                ownerModel,
                "Abrir panel", ctaUrl
        );
    }

    @Override
    @Async
    public void notifyBookingCancelledToUser(Booking booking, Club club, ClubTable table, Locale locale) {
        Objects.requireNonNull(booking, "booking cannot be null");
        Objects.requireNonNull(booking.getUser(), "booking.user cannot be null");
        Objects.requireNonNull(club, "club cannot be null");
        Objects.requireNonNull(table, "table cannot be null");

        String userEmail = booking.getUser().getEmail();
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new BadRequestException("User email is required for cancellation notification");
        }

        LocalDate selectedDate = booking.getBookingStart().atZoneSameInstant(ZONE_ID).toLocalDate();

        String subject = messageSource.getMessage("email.booking.cancelled", new Object[] { club.getClubName() }, locale);

        sendTemplatedEmail(
                userEmail,
                subject,
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
    public void notifyBookingCancelledToOwner(Booking booking, Club club, ClubTable table, User owner, Locale locale) {
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

        String subject = messageSource.getMessage("email.booking.cancelled", new Object[] { club.getClubName() }, locale);


        sendTemplatedEmail(
                owner.getEmail(),
                subject,
                "booking-cancelled-owner",
                ownerModel,
                "Ver panel",
                abs("/admin/clubs/bookings?userId=" + owner.getId())
        );
    }

    @Override
    @Async
    public void notifyReceiptUploaded(Booking booking, Club club, ClubTable table, User owner, Receipt receipt, Locale locale) {
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
        ownerModel.put("brandFooter", "© 2025 BlingBling.");

        String subject = messageSource.getMessage("email.receipt.uploaded", new Object[] { club.getClubName(), tableNum }, locale);

        sendTemplatedEmail(
                owner.getEmail(),
                subject,
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

    @Override
    @Async
    public void notifyConfirmationEmailToUser(Booking booking, Club club, ClubTable table, Locale locale) {
        Objects.requireNonNull(booking, "booking cannot be null");
        Objects.requireNonNull(booking.getUser(), "booking.user cannot be null");
        Objects.requireNonNull(club, "club cannot be null");
        Objects.requireNonNull(table, "table cannot be null");

        String toEmail = booking.getUser().getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            throw new BadRequestException("User email is required for notification");
        }

        LocalDate selectedDate = booking.getBookingStart().atZoneSameInstant(ZONE_ID).toLocalDate();

        String ctaUrl = abs("/booking?bookingId=" + booking.getBookingId());

        String subject = messageSource.getMessage("email.booking.confirmed.user.subject", new Object[] { club.getClubName() }, locale);

        sendTemplatedEmail(
                toEmail,
                subject,
                "booking-confirm-user",
                Map.of(
                        "clubName", club.getClubName(),
                        "tableNumber", table.getTableNumber(),
                        "capacity", table.getCapacity(),
                        "date", selectedDate.toString(),
                        "from", "23:59",
                        "to", "06:00",
                        "ctaUrl", ctaUrl
                ),
                "Ver reserva",
                ctaUrl
        );
    }

    @Override
    @Async
    public void sendOwnerReviewAlert(final Club club, final Review review, final Locale locale) {
        Objects.requireNonNull(club, "club cannot be null");
        Objects.requireNonNull(review, "review cannot be null");


        String ownerEmail = null;
        try {
            if (club.getContactInfo() != null && club.getContactInfo().getEmail() != null) {
                ownerEmail = club.getContactInfo().getEmail();
            }
        } catch (Exception ignore) {
        }

        if (ownerEmail == null || ownerEmail.isBlank()) {
            LOGGER.warn("sendOwnerReviewAlert: no owner email found for club {}", club.getClubName());
            return;
        }

        final String clubName   = Objects.requireNonNullElse(club.getClubName(), "Club");
        final String reviewer   = (review.getUser() != null && review.getUser().getUsername() != null)
                ? review.getUser().getUsername()
                : "usuario";
        final Integer rating    = review.getRating() != 0 ? review.getRating() : 5;
        final String description = Objects.requireNonNullElse(review.getDescription(), "");
        //final boolean fromTableGuest = Boolean.TRUE.equals(review.getFromTableGuest());

        String photoUrl = null;
        if (review.getPhotoId() != null) {
            photoUrl = abs("/photos/" + review.getPhotoId());
        }

        final OffsetDateTime createdOdt = toOffsetDateTime(review.getCreatedAt());
        final DateTimeFormatter fmt = REVIEW_DT_FMT(locale);

        final String createdAtPretty = (createdOdt != null)
                ? createdOdt.atZoneSameInstant(ZONE_ID).format(fmt)
                : OffsetDateTime.now(ZONE_ID).format(fmt);

        final String ctaUrl  = abs("/admin/clubs/handle?clubId=" + club.getClubId());
        final String ctaText = "Abrir reseñas";

        String subject;
        try {
            subject = messageSource.getMessage("email.review.owner.subject",
                    new Object[]{ clubName, rating }, locale);
        } catch (Exception e) {
            subject = "Nueva reseña (" + rating + "★) - " + clubName;
        }

        final Map<String, Object> model = new HashMap<>();
        model.put("clubName", clubName);
        model.put("reviewer", reviewer);
        model.put("rating", rating);
        model.put("description", description);
        model.put("fromTableGuest", true);
        model.put("photoUrl", photoUrl);
        model.put("createdAt", createdAtPretty);
        model.put("brandName", "BlingBling");
        model.put("brandFooter", "© 2025 BlingBling.");

        sendTemplatedEmail(
                ownerEmail,
                subject,
                "owner-review-alert",
                model,
                ctaText,
                ctaUrl
        );

        LOGGER.info("Owner review alert sent to {} for club {}", ownerEmail, clubName);
    }

    @Override
    @Async
    public void sendReviewRequestEmail(Booking booking, User user, Club club) {
        Locale locale = user.getLocale() != null ? Locale.forLanguageTag(user.getLocale()) : LocaleContextHolder.getLocale();
        String subject = messageSource.getMessage("email.review.request.subject", null, locale);
        String clubName = club != null ? club.getClubName() : "Club";
        Map<String, Object> model = Map.of(
                "username", user.getUsername(),
                "clubName", clubName,
                "date", booking.getBookingStart().toString()
        );
        sendTemplatedEmail(
                user.getEmail(),
                subject,
                "review-request",
                model,
                "Dejar reseña",
                abs("/review?bookingId=" + booking.getBookingId())
        );
    }


    private static DateTimeFormatter REVIEW_DT_FMT(Locale locale) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale);
    }


    private OffsetDateTime toOffsetDateTime(final Object value) {
        return switch (value) {
            case null -> null;
            case OffsetDateTime odt -> odt;
            case ZonedDateTime zdt -> zdt.toOffsetDateTime();
            case LocalDateTime ldt -> ldt.atZone(ZONE_ID).toOffsetDateTime();
            case LocalDate ld -> ld.atStartOfDay(ZONE_ID).toOffsetDateTime();
            case java.sql.Timestamp ts -> ts.toInstant().atZone(ZONE_ID).toOffsetDateTime();
            case Date d -> d.toInstant().atZone(ZONE_ID).toOffsetDateTime();
            default -> null;
        };

    }

    private boolean isLikelyEmail(String s) {
        return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

}