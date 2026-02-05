package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.AccountStatusDTO;
import com.dseme.app.dtos.users.UpdateProfileRequestDTO;
import com.dseme.app.dtos.users.UserDTO;
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
 * Controller for user profile management.
 * All endpoints require authentication and users can only access their own profile.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for user profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserAccountService userAccountService;

    /**
     * Get current user email from authentication context.
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile information")
    public ResponseEntity<UserDTO> getCurrentUserProfile() {
        String userEmail = getCurrentUserEmail();
        AccountStatusDTO status = userAccountService.getAccountStatus(userEmail);
        // Convert AccountStatusDTO to UserDTO
        UserDTO userDTO = UserDTO.builder()
                .id(status.getId())
                .email(status.getEmail())
                .firstName(status.getFirstName())
                .lastName(status.getLastName())
                .role(status.getRole())
                .isActive(status.getIsActive())
                .isVerified(status.getIsVerified())
                .createdAt(status.getCreatedAt())
                .build();
        return ResponseEntity.ok(userDTO);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Updates the authenticated user's firstName and lastName")
    public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO request) {
        String userEmail = getCurrentUserEmail();
        UserDTO updatedProfile = userAccountService.updateProfile(userEmail, request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PatchMapping("/profile")
    @Operation(summary = "Partially update user profile", description = "Partially updates the authenticated user's profile")
    public ResponseEntity<UserDTO> partialUpdateProfile(@RequestBody UpdateProfileRequestDTO request) {
        String userEmail = getCurrentUserEmail();
        UserDTO updatedProfile = userAccountService.updateProfile(userEmail, request);
        return ResponseEntity.ok(updatedProfile);
    }
}