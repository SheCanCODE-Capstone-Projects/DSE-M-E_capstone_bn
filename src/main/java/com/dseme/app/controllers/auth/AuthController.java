package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.ForgotPasswordDTO;
import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.LoginResponseDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.dtos.auth.ResetPasswordDTO;
import com.dseme.app.services.auth.AuthService;
import com.dseme.app.services.auth.EmailVerificationService;
import com.dseme.app.services.auth.OAuth2TokenStorage;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService verificationService;
    private final OAuth2TokenStorage tokenStorage;

    public AuthController(AuthService authService,
                          EmailVerificationService verificationService,
                          OAuth2TokenStorage tokenStorage) {
        this.authService = authService;
        this.verificationService = verificationService;
        this.tokenStorage = tokenStorage;
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        LoginResponseDTO response = authService.login(loginDTO);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        String result = verificationService.verifyEmail(token);
        
        if (result == null) {
            response.put("error", "Invalid verification token");
            return ResponseEntity.badRequest().body(response);
        }
        
        if ("already_verified".equals(result)) {
            response.put("message", "Email already verified. You can now login.");
            response.put("redirectTo", "/login");
            return ResponseEntity.ok(response);
        }
        
        if ("expired".equals(result)) {
            response.put("error", "Verification token expired");
            return ResponseEntity.badRequest().body(response);
        }
        
        if ("success".equals(result)) {
            response.put("message", "Email verified successfully!");
            response.put("redirectTo", "/login");
            return ResponseEntity.ok(response);
        }
        
        response.put("error", "Invalid or expired verification token");
        return ResponseEntity.badRequest().body(response);
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

    // ================= GOOGLE OAUTH2 =================
    /**
     * Endpoint to retrieve JWT token after Google OAuth2 authentication.
     * Also handles email verification messages and errors from OAuth flow.
     */
    @GetMapping("/google")
    public ResponseEntity<Map<String, Object>> getGoogleToken(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String error) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Handle error messages from OAuth flow (e.g., email not verified)
        if (error != null && !error.isEmpty()) {
            response.put("error", error);
            return ResponseEntity.badRequest().body(response);
        }
        
        // Handle success messages (e.g., email verification required)
        if (message != null && !message.isEmpty()) {
            response.put("message", message);
            return ResponseEntity.ok(response);
        }
        
        // Handle JWT token code
        if (code == null || code.isEmpty()) {
            response.put("error", "Missing code parameter");
            response.put("message", "Please provide a valid code from the OAuth2 redirect");
            return ResponseEntity.badRequest().body(response);
        }

        String token = tokenStorage.retrieveAndRemoveToken(code);
        
        if (token == null) {
            response.put("error", "Invalid or expired code");
            response.put("message", "The provided code is invalid or has already been used");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("token", token);
        response.put("message", "Google authentication successful");
        return ResponseEntity.ok(response);
    }
}
