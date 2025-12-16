package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.ForgotPasswordDTO;
import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.dtos.auth.ResetPasswordDTO;
import com.dseme.app.services.auth.AuthService;
import com.dseme.app.services.auth.EmailVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService verificationService;

    public AuthController(AuthService authService,
                          EmailVerificationService verificationService) {
        this.authService = authService;
        this.verificationService = verificationService;
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    // ================= FORGOT PASSWORD =================
    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        return authService.forgotPassword(dto);
    }

    // ================= RESET PASSWORD =================
    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        return authService.resetPassword(dto);
    }

    // ================= EMAIL VERIFICATION =================
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        boolean verified = verificationService.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok("Email verified successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired verification token");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@RequestParam String email) {
        try {
            verificationService.resendVerificationEmail(email);
            return ResponseEntity.ok("Verification email sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
