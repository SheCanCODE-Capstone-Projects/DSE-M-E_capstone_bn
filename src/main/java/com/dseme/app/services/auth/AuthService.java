package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.ForgotPasswordDTO;
import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.dtos.auth.ResetPasswordDTO;
import com.dseme.app.exceptions.AccountInactiveException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.utilities.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.security.SecureRandom;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // Add this line:
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepo,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       EmailService emailService) {
        this.userRepo = userRepo;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }


    public String register(RegisterDTO registerDTO) {
        if (userRepo.existsByEmail(registerDTO.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User with email '" + registerDTO.getEmail() + "' already exists!"
            );
        }

        User user = new User();
        user.setEmail(registerDTO.getEmail());
        user.setPasswordHash(encoder.encode(registerDTO.getPassword()));
        user.setRole(Role.UNASSIGNED);
        user.setIsActive(true);

        userRepo.save(user);

        return user.getEmail() + " has been successfully registered!";
    }

    public String login(LoginDTO loginDTO) {
        User user = userRepo.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Invalid email or password"
                ));

        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new AccountInactiveException("Your account is not active");
        }

        if (!encoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getEmail(),
                        loginDTO.getPassword()
                )
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return jwtUtil.generateToken(userDetails.getUsername());
    }

    // --- Forgot password ---
    public String forgotPassword(ForgotPasswordDTO dto) {
        User user = userRepo.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("If an account with this email exists, a reset code has been sent"));

        // Generate 6-digit numeric token
        String token = String.format("%06d", random.nextInt(1_000_000));

        user.setResetToken(token);
        user.setResetTokenExpiry(Instant.now().plusSeconds(120)); // 2 minutes expiry
        userRepo.save(user);

        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), token);
        return "Password reset email sent";
    }

    // --- Reset password ---
    public String resetPassword(ResetPasswordDTO dto) {
        User user = userRepo.findByResetToken(dto.getToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BadCredentialsException("Token expired");
        }

        user.setPasswordHash(encoder.encode(dto.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepo.save(user);

        return "Password reset successful";
    }
}