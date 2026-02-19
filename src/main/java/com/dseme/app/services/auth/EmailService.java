package com.dseme.app.services.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.brevo.api-key}")
    private String apiKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    // ========================= CORE SEND METHOD =========================

    private void sendEmail(String to, String subject, String htmlContent) {

        logger.info("Sending email to: {}", to);

        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", apiKey);

        Map<String, Object> body = new HashMap<>();

        body.put("sender", Map.of(
                "email", fromEmail,
                "name", "DSE Team"
        ));

        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(BREVO_URL, request, String.class);

            logger.info("Email sent successfully to {}. Brevo response: {}", to, response.getBody());

        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via Brevo API", e);
        }
    }

    // ========================= YOUR EXISTING METHODS =========================

    public void sendPasswordResetCode(String to, String code) {

        String htmlContent =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<body>" +
                        "<h2>Password Reset Code</h2>" +
                        "<p>Your 6-digit code is:</p>" +
                        "<h1 style='letter-spacing:5px;'>" + code + "</h1>" +
                        "<p><strong>This code expires in 2 minutes.</strong></p>" +
                        "</body>" +
                        "</html>";

        sendEmail(to, "DSE Password Reset Code", htmlContent);
    }

    public void sendVerificationEmail(String to, String token) {

        String verificationLink = frontendUrl + "/verify?token=" + token;

        String htmlContent =
                "<h2>Welcome to DSE!</h2>" +
                        "<p>Please verify your email:</p>" +
                        "<a href='" + verificationLink + "'>Verify Email</a>" +
                        "<p>This link expires in 24 hours.</p>";

        sendEmail(to, "Verify Your DSE Email Address", htmlContent);
    }

    public void sendPasswordResetLink(String to, String resetToken) {

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        String htmlContent =
                "<h2>Password Reset</h2>" +
                        "<p>Click below to reset your password:</p>" +
                        "<a href='" + resetLink + "'>Reset Password</a>" +
                        "<p>This link expires in 15 minutes.</p>";

        sendEmail(to, "Reset Your DSE Password", htmlContent);
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent);
    }
}
