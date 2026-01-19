package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.services.meofficer.MEOfficerNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for ME_OFFICER notification management operations.
 */
@Tag(name = "ME Officer Notifications", description = "Notification management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/notifications")
@RequiredArgsConstructor
public class MEOfficerNotificationController extends MEOfficerBaseController {

    private final MEOfficerNotificationService notificationService;

    /**
     * Gets all notifications for ME_OFFICER with filtering and pagination.
     * 
     * GET /api/me-officer/notifications
     */
    @Operation(
            summary = "Get notifications",
            description = "Retrieves all notifications for ME_OFFICER with optional filtering by type, priority, and read status. " +
                    "Includes pagination and unread count."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping
    public ResponseEntity<NotificationListResponseDTO> getNotifications(
            HttpServletRequest request,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by notification type") @RequestParam(required = false) NotificationType notificationType,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) Priority priority,
            @Parameter(description = "Filter by read status") @RequestParam(required = false) Boolean isRead
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        NotificationListResponseDTO response = notificationService.getNotifications(
                context, page, size, notificationType, priority, isRead);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a notification as read.
     * 
     * PATCH /api/me-officer/notifications/{notificationId}/read
     */
    @Operation(
            summary = "Mark notification as read",
            description = "Marks a specific notification as read."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "404", description = "Not Found - Notification not found")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            HttpServletRequest request,
            @Parameter(description = "Notification ID") @PathVariable UUID notificationId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        notificationService.markNotificationAsRead(context, notificationId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Marks multiple notifications as read.
     * 
     * PATCH /api/me-officer/notifications/mark-read
     */
    @Operation(
            summary = "Mark multiple notifications as read",
            description = "Marks multiple notifications as read in a single request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    @PatchMapping("/mark-read")
    public ResponseEntity<Void> markNotificationsAsRead(
            HttpServletRequest request,
            @Parameter(description = "List of notification IDs") @RequestBody List<UUID> notificationIds
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        notificationService.markNotificationsAsRead(context, notificationIds);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Gets notification preferences.
     * 
     * GET /api/me-officer/notifications/preferences
     */
    @Operation(
            summary = "Get notification preferences",
            description = "Retrieves notification preferences for ME_OFFICER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesDTO> getNotificationPreferences(
            HttpServletRequest request
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        NotificationPreferencesDTO preferences = notificationService.getNotificationPreferences(context);
        
        return ResponseEntity.ok(preferences);
    }

    /**
     * Updates notification preferences.
     * 
     * PUT /api/me-officer/notifications/preferences
     */
    @Operation(
            summary = "Update notification preferences",
            description = "Updates notification preferences for ME_OFFICER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token")
    })
    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesDTO> updateNotificationPreferences(
            HttpServletRequest request,
            @RequestBody NotificationPreferencesDTO preferences
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        NotificationPreferencesDTO updated = notificationService.updateNotificationPreferences(context, preferences);
        
        return ResponseEntity.ok(updated);
    }
}
