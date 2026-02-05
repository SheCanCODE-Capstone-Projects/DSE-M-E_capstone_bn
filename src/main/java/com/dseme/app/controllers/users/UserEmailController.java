package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.UpdateEmailRequestDTO;
import com.dseme.app.services.users.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for email management.
 * All endpoints require authentication and users can only change their own email.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Email", description = "Endpoints for email management")
@SecurityRequirement(name = "bearerAuth")
public class UserEmailController {

    private final UserAccountService userAccountService;

    /**
     * Get current user email from authentication context.
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @PutMapping("/email")
    @Operation(summary = "Request email change", description = "Requests to change the authenticated user's email. Verification email will be sent to the new address.")
    public ResponseEntity<String> requestEmailChange(@Valid @RequestBody UpdateEmailRequestDTO request) {
        String userEmail = getCurrentUserEmail();
        userAccountService.requestEmailChange(userEmail, request);
        return ResponseEntity.ok("Email change request sent. Please check your new email for verification.");
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Verify email change", description = "Verifies and completes the email change using the token sent to the new email address")
    public ResponseEntity<String> verifyEmailChange(@RequestParam String token) {
        userAccountService.verifyEmailChange(token);
        return ResponseEntity.ok("Email changed successfully. Please verify your new email address.");
    }
}
