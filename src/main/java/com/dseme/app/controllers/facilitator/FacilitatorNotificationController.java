package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.SendNotificationDTO;
import com.dseme.app.dtos.notifications.NotificationDTO;
import com.dseme.app.services.facilitator.FacilitatorNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for facilitator notification management.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Notifications", description = "APIs for managing notifications")
@RestController
@RequestMapping("/api/facilitator/notifications")
@RequiredArgsConstructor
public class FacilitatorNotificationController extends FacilitatorBaseController {

    private final FacilitatorNotificationService notificationService;

    /**
     * Sends notifications to participants.
     * 
     * POST /api/facilitator/notifications/send
     */
    @Operation(
        summary = "Send notifications to participants",
        description = "Sends notifications to one or more participants in the facilitator's active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/send")
    public ResponseEntity<String> sendNotifications(
            HttpServletRequest request,
            @Valid @RequestBody SendNotificationDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        notificationService.sendNotificationsToParticipants(context, dto);
        
        return ResponseEntity.ok("Notifications sent successfully");
    }

    /**
     * Gets all notifications for the facilitator.
     * 
     * GET /api/facilitator/notifications
     */
    @Operation(
        summary = "Get facilitator notifications",
        description = "Retrieves all notifications for the logged-in facilitator."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<NotificationDTO> notifications = notificationService.getFacilitatorNotifications(context);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marks a notification as read.
     * 
     * PUT /api/facilitator/notifications/{notificationId}/read
     */
    @Operation(
        summary = "Mark notification as read",
        description = "Marks a specific notification as read."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notification marked as read"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            HttpServletRequest request,
            @Parameter(description = "Notification ID") @PathVariable UUID notificationId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        notificationService.markNotificationAsRead(notificationId, context);
        
        return ResponseEntity.ok().build();
    }
}

