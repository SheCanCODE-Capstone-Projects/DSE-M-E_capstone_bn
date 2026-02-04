package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.NotificationListResponseDTO;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.services.donor.DonorNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for DONOR notification management.
 */
@Tag(name = "Donor Notifications", description = "Notification management endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorNotificationController extends DonorBaseController {

    private final DonorNotificationService notificationService;

    /**
     * Gets all notifications for DONOR users with pagination and filtering.
     * 
     * GET /api/donor/notifications
     * 
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - notificationType: Optional filter by notification type
     * - priority: Optional filter by priority
     * - isRead: Optional filter by read status (true/false)
     */
    @Operation(
            summary = "Get notifications",
            description = "Retrieves all notifications for DONOR users with pagination and filtering. " +
                    "Supports filtering by notification type, priority, and read status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping
    public ResponseEntity<NotificationListResponseDTO> getNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) NotificationType notificationType,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean isRead
    ) {
        DonorContext context = getDonorContext(request);
        
        NotificationListResponseDTO notifications = notificationService.getNotifications(
                context, page, size, notificationType, priority, isRead
        );
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marks a notification as read.
     * 
     * PATCH /api/donor/notifications/{id}/read
     */
    @Operation(
            summary = "Mark notification as read",
            description = "Marks a specific notification as read."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            HttpServletRequest request,
            @PathVariable UUID id
    ) {
        DonorContext context = getDonorContext(request);
        
        notificationService.markAsRead(context, id);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Marks all notifications as read for DONOR users.
     * 
     * PATCH /api/donor/notifications/read-all
     */
    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all notifications as read for all DONOR users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        notificationService.markAllAsRead(context);
        
        return ResponseEntity.ok().build();
    }
}
