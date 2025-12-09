package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.ForgotPasswordDTO;
import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.dtos.auth.ResetPasswordDTO;
import com.dseme.app.services.auth.AuthService;
import com.dseme.app.services.auth.EmailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    public AuthController(AuthService authService, EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        return authService.forgotPassword(dto);
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam("token") String token) {
        System.out.println("Token received: " + token);
        return "Token received: " + token;
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        return authService.resetPassword(dto);
    }

}
