package ar.edu.itba.paw.service;

import ar.edu.itba.paw.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final TaskExecutor mailExecutor;

    @Value("${app.mail.from:noreply@mi-dominio.com}")
    private String defaultFrom;

    public EmailServiceImpl(JavaMailSender mailSender,
                            TemplateEngine templateEngine,
                            TaskExecutor mailExecutor) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailExecutor = mailExecutor;
    }

    @Override
    @Async("mailExecutor")
    public CompletableFuture<Void> sendTemplatedEmail(String to, String subject, String templateName,
                                                      Map<String, Object> model, String ctaText, String ctaUrl) {
        try {
            Context ctx = new Context();
            Map<String, Object> m = new HashMap<>(model != null ? model : Map.of());
            m.put("ctaText", ctaText);
            m.put("ctaUrl", ctaUrl);
            ctx.setVariables(m);

            String html = templateEngine.process("email/" + templateName, ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(defaultFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mime);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            cf.completeExceptionally(e);
            return cf;
        }
    }


    @Override
    public CompletableFuture<Void> sendBookingInquiry(String to, Map<String, Object> model, String ctaUrl) {
        return sendTemplatedEmail(
                to,
                "Consulta recibida",
                "booking-inquiry",
                model,
                "Responder consulta",
                ctaUrl
        );
    }

    @Override
    public CompletableFuture<Void> sendBookingConfirmation(String to, Map<String, Object> model, String ctaUrl) {
        return sendTemplatedEmail(
                to,
                "Confirmación de reserva",
                "booking-confirmation",
                model,
                "Ver mi reserva",
                ctaUrl
        );
    }
}
