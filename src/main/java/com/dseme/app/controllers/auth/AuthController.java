package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
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

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    @GetMapping ("/test")
    public String test() {
        System.out.println("test starting");
        return "test";
    }
}
