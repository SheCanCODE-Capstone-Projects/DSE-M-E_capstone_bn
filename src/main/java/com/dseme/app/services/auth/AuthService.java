package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.ForgotPasswordDTO;
import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.LoginResponseDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.dtos.auth.ResetPasswordDTO;
import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccountInactiveException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.models.Forgotpassword;
import com.dseme.app.models.User;
import com.dseme.app.repositories.ForgotPasswordRepository;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.utilities.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final ForgotPasswordRepository forgotPasswordRepo;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepo,
            ForgotPasswordRepository forgotPasswordRepo,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            EmailService emailService,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepo = userRepo;
        this.forgotPasswordRepo = forgotPasswordRepo;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.emailVerificationService = emailVerificationService;
    }

    // ================= REGISTER =================
    @Transactional
    public String register(RegisterDTO dto) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);
        logger.info("Registering new user with email: {}", dto.getEmail());

        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User with email '" + dto.getEmail() + "' already exists"
            );
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(encoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName() != null && !dto.getFirstName().trim().isEmpty() 
                ? dto.getFirstName().trim() : "User");
        user.setLastName(dto.getLastName() != null && !dto.getLastName().trim().isEmpty() 
                ? dto.getLastName().trim() : "Account");
        user.setRole(Role.UNASSIGNED);
        user.setIsActive(true);
        user.setIsVerified(false); // Require email verification
        user.setProvider(Provider.LOCAL);

        User savedUser = userRepo.save(user);
        logger.info("User saved successfully: {}", savedUser.getId());
        
        try {
            emailVerificationService.generateAndSendVerificationToken(savedUser);
            logger.info("Verification token sent successfully for: {}", savedUser.getEmail());
            return "Registration successful. Please check your email to verify your account.";
        } catch (Exception e) {
            logger.error("Failed to generate/send verification token: {}", e.getMessage(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    // ================= LOGIN =================
    public LoginResponseDTO login(LoginDTO dto) {

        User user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadCredentialsException("Please verify your email first");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive");
        }

        // ME Officers MUST be associated with an organization
        if (user.getRole() == Role.ME_OFFICER && user.getPartner() == null) {
            throw new BadCredentialsException("ME Officer account must be associated with an organization. Please contact your administrator.");
        }

        // Facilitators MUST be associated with an organization
        if (user.getRole() == Role.FACILITATOR && user.getPartner() == null) {
            throw new BadCredentialsException("Facilitator account must be associated with an organization. Please contact your administrator.");
        }

        if (!encoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails.getUsername());
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);
        logger.info("Login - User: {}, Role: {}, Partner: {}, Center: {}",
                user.getEmail(),
                user.getRole(),
                user.getPartner() != null ? user.getPartner().getPartnerName() : "NULL",
                user.getCenter() != null ? user.getCenter().getCenterName() : "NULL");
        
        return LoginResponseDTO.builder()
                .token(token)
                .userId(user.getId().toString())
                .role(user.getRole().name())
                .redirectTo(getRedirectUrl(user.getRole()))
                .message(user.getRole() == Role.UNASSIGNED ? "Please request a role to access the system" : "Login successful")
                .organizationName(user.getPartner() != null ? user.getPartner().getPartnerName() : null)
                .organizationId(user.getPartner() != null ? user.getPartner().getPartnerId() : null)
                .locationName(user.getCenter() != null ? user.getCenter().getCenterName() : null)
                .locationId(user.getCenter() != null ? user.getCenter().getId().toString() : null)
                .build();
    }
    
    private String getRedirectUrl(Role role) {
        switch (role) {
            case UNASSIGNED:
                return "/request-access";
            case FACILITATOR:
                return "/facilitator/overview";
            case ME_OFFICER:
                return "/ME/overviews";
            case DONOR:
                return "/donor/overview";
            default:
                return "/request-access";
        }
    }

    // ================= FORGOT PASSWORD =================
    @Transactional
    public String forgotPassword(ForgotPasswordDTO dto) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthService.class);
        logger.info("Processing forgot password request for email: {}", dto.getEmail());

        User user = userRepo.findByEmail(dto.getEmail()).orElse(null);

        if (user != null) {
            logger.info("User found: {}", dto.getEmail());
            forgotPasswordRepo.deleteByUser(user);

            int tokenInt = 100000 + random.nextInt(900000);
            String token = String.valueOf(tokenInt);

            Forgotpassword fp = new Forgotpassword();
            fp.setToken(token);
            fp.setUser(user);
            fp.setExpirationTime(new Date(System.currentTimeMillis() + 2 * 60 * 1000));

            Forgotpassword savedFp = forgotPasswordRepo.save(fp);
            logger.info("Password reset token saved. Token ID: {}, Email: {}", savedFp.getId(), user.getEmail());
            
            try {
                logger.info("Attempting to send password reset code to: {}", user.getEmail());
                emailService.sendPasswordResetCode(user.getEmail(), token, user.getFirstName());
                logger.info("Password reset code email sent successfully to: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to send password reset code email to {}: {}", user.getEmail(), e.getMessage(), e);
                throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
            }
        } else {
            logger.info("No user found with email: {}. Not revealing this for security.", dto.getEmail());
        }

        return "If an account exists, a reset code has been sent";
    }

    // ================= RESET PASSWORD =================
    @Transactional
    public String resetPassword(ResetPasswordDTO dto) {

        Forgotpassword fp = forgotPasswordRepo
                .findByToken(dto.getToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));

        if (fp.getExpirationTime().before(new Date())) {
            forgotPasswordRepo.delete(fp);
            throw new BadCredentialsException("Token expired");
        }

        User user = fp.getUser();
        user.setPasswordHash(encoder.encode(dto.getNewPassword()));
        userRepo.save(user);

        forgotPasswordRepo.delete(fp);

        return "Password reset successful";
    }

    // ================= DEBUG HELPER =================
    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email).orElse(null);
    }
}