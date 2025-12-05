package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.LoginDTO;
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
    public String signUp(@Valid @RequestBody SignUpDTO signUpDTO) {
        return authService.signUp(signUpDTO);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    @GetMapping ("/test")
    public String test() {
        return "test";
    }
}
