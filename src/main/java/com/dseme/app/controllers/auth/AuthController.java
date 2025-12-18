package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.RegisterDTO;
import com.dseme.app.services.auth.AuthService;
import com.dseme.app.services.auth.OAuth2TokenStorage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2TokenStorage tokenStorage;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterDTO registerDTO) {
        return authService.register(registerDTO);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO);
    }

    @GetMapping("/google")
    public ResponseEntity<Map<String, Object>> getGoogleToken(@RequestParam(required = false) String code) {
        Map<String, Object> response = new HashMap<>();
        
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
