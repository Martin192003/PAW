package ar.edu.itba.paw.service;

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
}