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
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailVerificationService.class);
        logger.info("Starting token generation for user: {}", user.getEmail());
        
        // Check rate limiting
        if (isRateLimited(user.getEmail())) {
            throw new RuntimeException("Please wait " + RATE_LIMIT_MINUTES + " minutes before requesting another verification email");
        }

        // Delete existing tokens
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();
        logger.info("Deleted existing tokens for user: {}", user.getEmail());

        // Generate new token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(24 * 60 * 60)) // 24 hours
                .build();

        EmailVerificationToken savedToken = tokenRepository.save(verificationToken);
        tokenRepository.flush();
        logger.info("Saved verification token ID: {} for email: {} with token: {}", savedToken.getId(), user.getEmail(), token);

        // Send email (with personalized greeting when firstName is set)
        try {
            emailService.sendVerificationEmail(user.getEmail(), token, user.getFirstName());
            logger.info("Verification email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
        
        // Update rate limit
        rateLimitMap.put(user.getEmail(), Instant.now());
    }

    public String verifyEmail(String token) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailVerificationService.class);
        logger.info("Attempting to verify token: {}", token);
        
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            logger.warn("Token not found in database: {}", token);
            logger.info("Total tokens in database: {}");
            return null; // Token not found
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        User user = verificationToken.getUser();
        logger.info("Token found for user: {}", user.getEmail());
        
        // Check if already verified
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            logger.info("User already verified: {}", user.getEmail());
            return "already_verified";
        }
        
        // Check if token expired
        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            logger.warn("Token expired for user: {}", user.getEmail());
            return "expired";
        }

        // Mark user as verified
        user.setIsVerified(true);
        userRepository.save(user);
        logger.info("User marked as verified: {}", user.getEmail());

        // Delete the token
        tokenRepository.delete(verificationToken);
        logger.info("Token deleted for user: {}", user.getEmail());
        
        return "success";
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