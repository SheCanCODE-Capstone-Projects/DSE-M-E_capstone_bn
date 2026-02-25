package com.dseme.app.controllers.auth;

import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugAuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public DebugAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Debug endpoint to check user status
     * Usage: GET /api/debug/user-status?email=user@example.com
     */
    @GetMapping("/user-status")
    public ResponseEntity<Map<String, Object>> checkUserStatus(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            response.put("exists", false);
            response.put("message", "User not found in database");
            return ResponseEntity.ok(response);
        }
        
        response.put("exists", true);
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("isActive", user.getIsActive());
        response.put("isVerified", user.getIsVerified());
        response.put("hasPartner", user.getPartner() != null);
        response.put("partnerName", user.getPartner() != null ? user.getPartner().getPartnerName() : null);
        response.put("hasCenter", user.getCenter() != null);
        response.put("provider", user.getProvider());
        response.put("passwordHashPrefix", user.getPasswordHash() != null ? user.getPasswordHash().substring(0, 7) : null);
        
        // Check login eligibility
        boolean canLogin = true;
        String reason = "User can login";
        
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            canLogin = false;
            reason = "Email not verified";
        } else if (!Boolean.TRUE.equals(user.getIsActive())) {
            canLogin = false;
            reason = "Account inactive";
        } else if ((user.getRole().name().equals("ME_OFFICER") || user.getRole().name().equals("FACILITATOR")) 
                   && user.getPartner() == null) {
            canLogin = false;
            reason = "Role requires organization but none assigned";
        }
        
        response.put("canLogin", canLogin);
        response.put("loginBlockReason", reason);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Debug endpoint to test password
     * Usage: POST /api/debug/test-password
     * Body: {"email": "user@example.com", "password": "testpass"}
     */
    @PostMapping("/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String email = request.get("email");
        String password = request.get("password");
        
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            response.put("userExists", false);
            response.put("message", "User not found");
            return ResponseEntity.ok(response);
        }
        
        boolean passwordMatches = encoder.matches(password, user.getPasswordHash());
        
        response.put("userExists", true);
        response.put("passwordMatches", passwordMatches);
        response.put("hashPrefix", user.getPasswordHash().substring(0, 7));
        
        if (!passwordMatches) {
            response.put("message", "Password does not match stored hash");
        } else {
            response.put("message", "Password is correct");
        }
        
        return ResponseEntity.ok(response);
    }
}
