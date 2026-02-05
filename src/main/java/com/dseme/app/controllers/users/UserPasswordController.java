package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.ChangePasswordRequestDTO;
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
 * Controller for password management.
 * All endpoints require authentication and users can only change their own password.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Password", description = "Endpoints for password management")
@SecurityRequirement(name = "bearerAuth")
public class UserPasswordController {

    private final UserAccountService userAccountService;

    /**
     * Get current user email from authentication context.
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @PutMapping("/password")
    @Operation(summary = "Change password", description = "Changes the authenticated user's password. Requires current password.")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request) {
        String userEmail = getCurrentUserEmail();
        userAccountService.changePassword(userEmail, request);
        return ResponseEntity.ok("Password changed successfully");
    }
}
