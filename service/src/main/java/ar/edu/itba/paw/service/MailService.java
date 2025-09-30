package ar.edu.itba.paw.service;

/*
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;
    private final String defaultFrom;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

    public boolean sendContact(String to, String clubName, String fromEmail, String name, String phone, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(defaultFrom);
            mail.setTo(to);
            mail.setSubject("Contacto para " + clubName);
            mail.setReplyTo(fromEmail);
            mail.setText(buildBody(name, fromEmail, phone, message));

            LOGGER.info("SMTP send attempt host={} user={} from={} to={} subject={}",
                    host(), user(), defaultFrom, to, mail.getSubject());
            mailSender.send(mail);
            LOGGER.info("SMTP send OK to {}", to);
            return true;
        } catch (Exception e) {
            LOGGER.error("SMTP send FAILED to {}: {}", to, e.getMessage(), e);
            return false;
        }
    }

    public void sendContactConfirmation(String userEmail, String clubName, String originalMessage) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(defaultFrom);
            mail.setTo(userEmail);
            mail.setSubject("Confirmación de contacto - " + clubName);
            mail.setText("Hemos recibido tu mensaje para " + clubName + ".\n\nCopia de tu mensaje:\n\""
                    + originalMessage + "\"\n\nTe responderán a la brevedad.\n\n(No responder a este correo)");
            LOGGER.info("SMTP confirmation send attempt to {}", userEmail);
            mailSender.send(mail);
            LOGGER.info("SMTP confirmation OK to {}", userEmail);
        } catch (Exception e) {
            LOGGER.warn("Confirmation email FAILED to {}: {}", userEmail, e.getMessage());
        }
    }

    private String host() {
        return (mailSender instanceof JavaMailSenderImpl)
                ? ((JavaMailSenderImpl) mailSender).getHost() : "?";
    }

    private String user() {
        return (mailSender instanceof JavaMailSenderImpl)
                ? ((JavaMailSenderImpl) mailSender).getUsername() : "?";
    }

    private String buildBody(String name, String email, String phone, String msg) {
        return "Nombre: " + name + "\nEmail: " + email +
                (phone == null || phone.isEmpty() ? "" : "\nTel: " + phone) +
                "\n\nMensaje:\n" + msg;
    }
}*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.context.i18n.LocaleContextHolder;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;


@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String defaultFrom;

    public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
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

    @Async
    public void sendClubCreationConfirmation(String to, String clubName){
        try{
            Locale userLocale = LocaleContextHolder.getLocale();

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject("Confirmación de creación de club - " + clubName);

            Context context = new Context(userLocale);
            context.setVariable("clubName", clubName);
            context.setVariable("createdAt", ZonedDateTime.now());

            String htmlBody = templateEngine.process("club-creation-confirmation", context);
            helper.setText(htmlBody, true);

            mailSender.send(mail);
            LOGGER.info("SMTP club creation confirmation OK to {}", to);
        } catch (MessagingException e) {
            LOGGER.error("SMTP club creation confirmation FAILED to {}: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendUserRegistrationConfirmation(String to, String username, String verificationToken){
        try{
            Locale userLocale = LocaleContextHolder.getLocale();

            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);
            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject("Confirmación de registro - " + username);

            Context context = new Context(userLocale);
            context.setVariable("username", username);
            context.setVariable("registeredAt", ZonedDateTime.now());
            context.setVariable("verificationUrl", "http://pawserver.it.itba.edu.ar/paw-2025b-06/verify?token=" + verificationToken);

            String htmlBody = templateEngine.process("user-registration-confirmation", context);
            helper.setText(htmlBody, true);

            mailSender.send(mail);
            LOGGER.info("SMTP user registration confirmation OK to {}", to);
        } catch (MessagingException e) {
            LOGGER.error("SMTP user registration confirmation FAILED to {}: {}", to, e.getMessage(), e);
        }
    }

}