package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.ForgotPasswordDTO;
import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.dtos.auth.ResetPasswordDTO;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccountInactiveException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.models.EmailVerificationToken;
import com.dseme.app.models.Forgotpassword;
import com.dseme.app.models.User;
import com.dseme.app.repositories.EmailVerificationTokenRepository;
import com.dseme.app.services.auth.EmailVerificationService;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final ForgotPasswordRepository forgotPasswordRepo;
    private final EmailVerificationTokenRepository tokenRepo;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();

    public AuthService(
            UserRepository userRepo,
            ForgotPasswordRepository forgotPasswordRepo,
            EmailVerificationTokenRepository tokenRepo,
            AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            EmailService emailService,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepo = userRepo;
        this.forgotPasswordRepo = forgotPasswordRepo;
        this.tokenRepo = tokenRepo;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.emailVerificationService = emailVerificationService;
    }

    // ================= REGISTER =================
    @Transactional
    public String register(RegisterDTO dto) {

        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User with email '" + dto.getEmail() + "' already exists"
            );
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(encoder.encode(dto.getPassword()));
        user.setRole(Role.UNASSIGNED);
        user.setIsActive(false);
        user.setIsVerified(false);

        User savedUser = userRepo.save(user);
        
        // Generate and send verification email
        emailVerificationService.generateAndSendVerificationToken(savedUser);

        return "Registration successful. Please check your email to verify your account.";
    }

    // ================= LOGIN =================
    public String login(LoginDTO dto) {

        User user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadCredentialsException("Please verify your email first");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive");
        }

        if (!encoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        return jwtUtil.generateToken(userDetails.getUsername());
    }

    // ================= FORGOT PASSWORD =================
    @Transactional
    public String forgotPassword(ForgotPasswordDTO dto) {

        User user = userRepo.findByEmail(dto.getEmail()).orElse(null);

        if (user != null) {
            forgotPasswordRepo.deleteByUser(user);

            int tokenInt = 100000 + random.nextInt(900000);
            String token = String.valueOf(tokenInt);

            Forgotpassword fp = new Forgotpassword();
            fp.setToken(token);
            fp.setUser(user);
            fp.setExpirationTime(new Date(System.currentTimeMillis() + 2 * 60 * 1000));

            forgotPasswordRepo.save(fp);
            emailService.sendPasswordResetCode(user.getEmail(), token);
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
}
