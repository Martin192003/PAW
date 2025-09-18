package ar.edu.itba.paw.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.time.ZonedDateTime;
import java.util.UUID;


@Service
public class MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String defaultFrom;

    public MailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.from:}") String configuredFrom
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;

        String from = (configuredFrom != null && !configuredFrom.isBlank())
                ? configuredFrom
                : null;

        if ((from == null || from.isBlank()) && mailSender instanceof JavaMailSenderImpl) {
            String username = ((JavaMailSenderImpl) mailSender).getUsername();
            if (username != null && !username.isBlank()) {
                from = username;
            }
        }
        if (from == null || from.isBlank()) {
            from = "no-reply@local";
        }

        this.defaultFrom = from;
        LOGGER.info("MailService defaultFrom={} host={} user={}", this.defaultFrom, host(), user());
    }

    public boolean sendContact(String to, String clubName, String fromEmail, String name, String phone, String message) {
        try {
            // Email to club
            MimeMessage clubMail = mailSender.createMimeMessage();
            MimeMessageHelper clubHelper = new MimeMessageHelper(clubMail, true, "UTF-8");
            clubHelper.setFrom(defaultFrom);
            clubHelper.setTo(to);
            clubHelper.setSubject("Contacto para " + clubName);
            clubHelper.setReplyTo(fromEmail);

            Context clubContext = new Context();
            clubContext.setVariable("clubName", clubName);
            clubContext.setVariable("name", name);
            clubContext.setVariable("fromEmail", fromEmail);
            clubContext.setVariable("phone", phone);
            clubContext.setVariable("message", message);
            clubContext.setVariable("submittedAt", ZonedDateTime.now());
            String clubHtmlBody = templateEngine.process("booking-inquiry-club", clubContext);
            clubHelper.setText(clubHtmlBody, true);

            // Confirmation email to user
            MimeMessage userMail = mailSender.createMimeMessage();
            MimeMessageHelper userHelper = new MimeMessageHelper(userMail, true, "UTF-8");
            userHelper.setFrom(defaultFrom);
            userHelper.setTo(fromEmail);
            userHelper.setSubject("Confirmación de contacto - " + clubName);

            Context userContext = new Context();
            userContext.setVariable("clubName", clubName);
            userContext.setVariable("name", name);
            userContext.setVariable("submittedAt", ZonedDateTime.now());
            String ctaUrl = "" + UUID.randomUUID(); // TODO replace with deployment URL
            userContext.setVariable("ctaUrl", ctaUrl);
            userContext.setVariable("ctaText", "Confirmar mi correo");

            String userHtmlBody = templateEngine.process("booking-inquiry-user", userContext);
            userHelper.setText(userHtmlBody, true);

            // Send
            mailSender.send(clubMail);
            mailSender.send(userMail);

            LOGGER.info("SMTP send OK to {} and {}", to, fromEmail);
            return true;
        } catch (MessagingException e) {
            LOGGER.error("SMTP send FAILED: {}", e.getMessage(), e);
            return false;
        }
    }

    private String host() {
        return (mailSender instanceof JavaMailSenderImpl)
                ? ((JavaMailSenderImpl) mailSender).getHost()
                : "?";
    }

    private String user() {
        return (mailSender instanceof JavaMailSenderImpl)
                ? ((JavaMailSenderImpl) mailSender).getUsername()
                : "?";
    }
}