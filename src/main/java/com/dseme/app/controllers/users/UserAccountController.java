package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.AccountStatusDTO;
import com.dseme.app.services.users.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for account status and management.
 * All endpoints require authentication and users can only access their own account.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "Endpoints for account status and management")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private final UserAccountService userAccountService;

    /**
     * Get current user email from authentication context.
     */
    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @GetMapping("/status")
    @Operation(summary = "Get account status", description = "Returns the authenticated user's account status and information")
    public ResponseEntity<AccountStatusDTO> getAccountStatus() {
        String userEmail = getCurrentUserEmail();
        AccountStatusDTO status = userAccountService.getAccountStatus(userEmail);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate account", description = "Deactivates the authenticated user's account (soft delete)")
    public ResponseEntity<String> deactivateAccount() {
        String userEmail = getCurrentUserEmail();
        userAccountService.deactivateAccount(userEmail);
        return ResponseEntity.ok("Account deactivated successfully");
    }
}
