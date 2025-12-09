package com.dseme.app.services.auth;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("dseme.system@gmail.com");
        message.setSubject("DSE Password Reset Code");

        // Send token as plain 6-digit code instead of long UUID
        message.setText(
                "Hello,\n\n" +
                        "You requested to reset your password.\n" +
                        "Use the following 6-digit code to reset your password:\n\n" +
                        token + "\n\n" +
                        "This code will expire in 2 minutes.\n\n" +
                        "If you didn't request a password reset, please ignore this email.\n\n" +
                        "Thank you,\n" +
                        "DSE Team"
        );

        mailSender.send(message);
    }
}
