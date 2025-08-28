package ar.edu.itba.paw.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface EmailService {

    CompletableFuture<Void> sendTemplatedEmail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> model,
            String ctaText,
            String ctaUrl
    );

    CompletableFuture<Void> sendBookingInquiry(String to, Map<String, Object> model, String ctaUrl);

    CompletableFuture<Void> sendBookingConfirmation(String to, Map<String, Object> model, String ctaUrl);
}

