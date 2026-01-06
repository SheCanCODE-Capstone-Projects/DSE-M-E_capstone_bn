package com.dseme.app.services.auth;

import com.dseme.app.models.EmailVerificationToken;
import com.dseme.app.models.User;
import com.dseme.app.repositories.EmailVerificationTokenRepository;
import com.dseme.app.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    // Simple rate limiting - email -> last sent time
    private final ConcurrentHashMap<String, Instant> rateLimitMap = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MINUTES = 5;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                  UserRepository userRepository,
                                  EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public void generateAndSendVerificationToken(User user) {
        // Check rate limiting
        if (isRateLimited(user.getEmail())) {
            throw new RuntimeException("Please wait " + RATE_LIMIT_MINUTES + " minutes before requesting another verification email");
        }

        // Delete existing tokens
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        // Generate new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(24 * 60 * 60)) // 24 hours
                .build();

        tokenRepository.save(verificationToken);

        // Send email
        emailService.sendVerificationEmail(user.getEmail(), token);
        
        // Update rate limit
        rateLimitMap.put(user.getEmail(), Instant.now());
    }

    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        
        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            return false;
        }

        // Mark user as verified
        User user = verificationToken.getUser();
        user.setIsVerified(true);
        userRepository.save(user);

        // Delete the token
        tokenRepository.delete(verificationToken);
        
        return true;
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new RuntimeException("Email is already verified");
        }

        generateAndSendVerificationToken(user);
    }

    private boolean isRateLimited(String email) {
        Instant lastSent = rateLimitMap.get(email);
        if (lastSent == null) {
            return false;
        }
        
        return Instant.now().isBefore(lastSent.plusSeconds(RATE_LIMIT_MINUTES * 60));
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(Instant.now());
    }
}