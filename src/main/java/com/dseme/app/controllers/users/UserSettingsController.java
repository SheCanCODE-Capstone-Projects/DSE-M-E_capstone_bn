package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.UpdateSettingsRequestDTO;
import com.dseme.app.dtos.users.UserSettingsDTO;
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
 * Controller for user settings and preferences.
 * All endpoints require authentication and users can only access their own settings.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Settings", description = "Endpoints for user settings and preferences")
@SecurityRequirement(name = "bearerAuth")
public class UserSettingsController {

    private final UserAccountService userAccountService;

    /**
     * Get current user email from authentication context.
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/settings")
    @Operation(summary = "Get user settings", description = "Returns the authenticated user's settings and preferences")
    public ResponseEntity<UserSettingsDTO> getSettings() {
        String userEmail = getCurrentUserEmail();
        UserSettingsDTO settings = userAccountService.getSettings(userEmail);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    @Operation(summary = "Update user settings", description = "Updates the authenticated user's settings and preferences")
    public ResponseEntity<UserSettingsDTO> updateSettings(@Valid @RequestBody UpdateSettingsRequestDTO request) {
        String userEmail = getCurrentUserEmail();
        UserSettingsDTO updatedSettings = userAccountService.updateSettings(userEmail, request);
        return ResponseEntity.ok(updatedSettings);
    }
}
