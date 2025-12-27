package com.dseme.app.services.auth;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("DSE Password Reset Code");

            helper.setText("""
                        <p>Hello,</p>
                        <p>You requested to reset your password.</p>
                        <p>Use the following 6-digit code to reset your password:</p>
                        <h2>%s</h2>
                        <p>This code will expire in 2 minutes.</p>
                        <p>If you didn't request a password reset, please ignore this email.</p>
                        <p>Thank you,<br>DSE Team</p>
                    """.formatted(code), true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendVerificationEmail(String to, String token) {
        System.out.println("🚀 EmailService.sendVerificationEmail() called with to=" + to + ", token=" + token);
        
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setFrom(fromEmail);
            helper.setSubject("DSE Email Verification");

            // Get backend URL from environment or use default
            String backendUrl = System.getenv("BACKEND_URL");
            if (backendUrl == null || backendUrl.isEmpty()) {
                backendUrl = "http://localhost:8088";
            }
            
            String verificationLink = backendUrl + "/api/auth/verify?token=" + token;
            
            String htmlContent =
                    "<p>Hello,</p>"
                            + "<p>Please verify your email address by clicking the link below:</p>"
                            + "<p><a href=\"" + verificationLink + "\">Verify Email</a></p>"
                            + "<p>Or copy and paste this link in your browser:</p>"
                            + "<p>" + verificationLink + "</p>"
                            + "<p>This link will expire in 24 hours.</p>"
                            + "<p>If you didn't create an account, please ignore this email.</p>"
                            + "<p>Thank you,<br/>DSE Team</p>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
}

//    public void sendPasswordResetLink(String to, String link) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            helper.setFrom(fromEmail);
//            helper.setTo(to);
//            helper.setSubject("Reset your DSE password");
//
//            helper.setText("""
//                <p>Hello,</p>
//                <p>Click the button below to reset your password:</p>
//                <p>
//                  <a href="%s"
//                     style="padding:10px 20px;background:#0d6efd;color:white;text-decoration:none;border-radius:5px;">
//                     Reset Password
//                  </a>
//                </p>
//                <p>This link expires in 2 minutes.</p>
//            """.formatted(link), true);
//
//            mailSender.send(message);
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to send email", e);
//        }
