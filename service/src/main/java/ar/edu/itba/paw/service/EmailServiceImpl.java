package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String defaultFrom;
    private final UserService userDao;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine, UserService userDao) {
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

    @Override
    @Async
    public CompletableFuture<Void> sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> model, String ctaText, String ctaUrl) {
        return CompletableFuture.runAsync(() -> {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.setFrom(defaultFrom);
                helper.setTo(to);
                helper.setSubject(subject);

                Locale userLocale = LocaleContextHolder.getLocale();
                if (model != null && model.containsKey("userLocale")) {
                    userLocale = (java.util.Locale) model.get("userLocale");
                }

                Context context = new Context(userLocale);
                if (model != null) {
                    model.forEach(context::setVariable);
                }
                context.setVariable("ctaText", ctaText);
                context.setVariable("ctaUrl", ctaUrl);
                context.setVariable("sentAt", ZonedDateTime.now());

                String htmlBody = templateEngine.process(templateName, context);
                helper.setText(htmlBody, true);

                mailSender.send(mimeMessage);
                LOGGER.info("SMTP send OK to {}", to);
            } catch (MessagingException e) {
                LOGGER.error("SMTP send FAILED to {}: {}", to, e.getMessage(), e);
            }
        });
    }

    @Override
    @Async
    public boolean sendContact(String to, String clubName, String fromEmail, String name, String phone, String message) {
        try {
            Locale userLocale = LocaleContextHolder.getLocale();

            // First email using "booking-inquiry-club.html"
            MimeMessage clubMail = mailSender.createMimeMessage();
            MimeMessageHelper clubHelper = new MimeMessageHelper(clubMail, true);
            clubHelper.setFrom(defaultFrom);
            clubHelper.setTo(to);
            clubHelper.setSubject("Contacto para " + clubName);
            clubHelper.setReplyTo(fromEmail);

            Context clubContext = new Context(userLocale);
            clubContext.setVariable("clubName", clubName);
            clubContext.setVariable("name", name);
            clubContext.setVariable("fromEmail", fromEmail);
            clubContext.setVariable("phone", phone);
            clubContext.setVariable("message", message);
            clubContext.setVariable("submittedAt", ZonedDateTime.now());
            String clubHtmlBody = templateEngine.process("booking-inquiry-club", clubContext);
            clubHelper.setText(clubHtmlBody, true);

            // Second email using "booking-inquiry-user.html"
            MimeMessage userMail = mailSender.createMimeMessage();
            MimeMessageHelper userHelper = new MimeMessageHelper(userMail, true);
            userHelper.setFrom(defaultFrom);
            userHelper.setTo(fromEmail);
            userHelper.setSubject("Confirmación de contacto - " + clubName);

            Context userContext = new Context(userLocale);
            userContext.setVariable("clubName", clubName);
            userContext.setVariable("name", name);
            userContext.setVariable("submittedAt", ZonedDateTime.now());

            String ctaUrl = "" + UUID.randomUUID(); //Falta URL deployment
            userContext.setVariable("ctaUrl", ctaUrl);
            userContext.setVariable("ctaText", "Confirmar mi correo");

            String userHtmlBody = templateEngine.process("booking-inquiry-user", userContext);
            userHelper.setText(userHtmlBody, true);

            // Send both emails
            mailSender.send(clubMail);
            mailSender.send(userMail);

            LOGGER.info("SMTP send OK to {} and {}", to, fromEmail);
            return true;
        } catch (MessagingException e) {
            LOGGER.error("SMTP send FAILED: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Async
    public void sendBookingConfirmationToUser(User user, Club club, Table table, Booking booking) {
        final SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setSubject("Confirmación de solicitud de reserva - " + club.getClubName());
        msg.setText("""
                ¡Hola %s!

                Recibimos tu solicitud de reserva en %s.

                Detalles:
                - Mesa: #%d (capacidad %d)
                - Horario: %s a %s
                - Estado: %s
                - Total: %s

                Te avisaremos cuando se confirme. ¡Gracias!
                """.formatted(
                user.getUsername(),
                club.getClubName(),
                table.getTableNumber(), table.getCapacity(),
                booking.getBookingStart(), booking.getBookingEnd(),
                booking.getStatus(), booking.getTotalAmount()
        ));
        mailSender.send(msg);
    }

    @Override
    @Async
    public void sendBookingNotificationToOwner(Club club, Table table, Booking booking) {
        // Asumo club.getContactInfo() referencia a un user_id dueño/manager
        User owner = null;
        if (club.getContactInfo() != null) {
            owner = userDao.findById(club.getContactInfo().getId());
        }
        final String ownerMail = owner != null ? owner.getEmail() : "owner@" + club.getClubName().toLowerCase().replace(" ","") + ".com";

        final SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(ownerMail);
        msg.setSubject("Nueva reserva (PENDIENTE) - " + club.getClubName());
        msg.setText("""
                Nueva solicitud de reserva:
                - Mesa: #%d
                - Horario: %s a %s
                - Total: %s
                - Estado actual: %s
                - Booking ID: %s

                Podés confirmarla desde el panel de reservas.
                """.formatted(
                table.getTableNumber(),
                booking.getBookingStart(), booking.getBookingEnd(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getBookingId()
        ));
        mailSender.send(msg);
    }

}