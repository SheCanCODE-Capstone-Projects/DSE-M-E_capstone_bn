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
        user.setIsVerified(false);
        user.setProvider(Provider.LOCAL);

        User savedUser = userRepo.save(user);
        
        try {
            emailVerificationService.generateAndSendVerificationToken(savedUser);
            return "Registration successful. Please check your email to verify your account.";
        } catch (Exception e) {
            return "Registration successful. Verification email will be sent shortly.";
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

        if (!encoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails.getUsername());
        
        return LoginResponseDTO.builder()
                .token(token)
                .role(user.getRole().name())
                .redirectTo(getRedirectUrl(user.getRole()))
                .message(user.getRole() == Role.UNASSIGNED ? "Please request a role to access the system" : "Login successful")
                .build();
    }
    
    private String getRedirectUrl(Role role) {
        switch (role) {
            case UNASSIGNED:
                return "/request-access";
            case ADMIN:
                return "/admin/dashboard";
            case FACILITATOR:
                return "/facilitator/dashboard";
            case ME_OFFICER:
                return "/me/dashboard";
            case DONOR:
                return "/donor/dashboard";
            default:
                return "/request-access";
        }
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
