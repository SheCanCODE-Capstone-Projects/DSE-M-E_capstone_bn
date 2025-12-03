package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.SignUpDTO;
import com.dseme.app.services.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public String signUp (@Valid @RequestBody SignUpDTO signUpDTO) {
        return this.authService.signUp(signUpDTO);
    }
}