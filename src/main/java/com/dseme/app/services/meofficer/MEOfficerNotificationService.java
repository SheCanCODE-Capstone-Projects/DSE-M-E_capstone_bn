package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Notification;
import com.dseme.app.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER notification management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerNotificationService {

    private final NotificationRepository notificationRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Gets all notifications for ME_OFFICER with filtering and pagination.
     * 
     * @param context ME_OFFICER context
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param notificationType Filter by notification type (optional)
     * @param priority Filter by priority (optional)
     * @param isRead Filter by read status (optional)
     * @return Paginated notification list
     */
    public NotificationListResponseDTO getNotifications(
            MEOfficerContext context,
            int page,
            int size,
            NotificationType notificationType,
            Priority priority,
            Boolean isRead
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get all notifications for ME_OFFICER
        List<Notification> allNotifications = notificationRepository.findByRecipient(context.getMeOfficer());

        // Apply filters
        List<Notification> filtered = allNotifications.stream()
                .filter(n -> notificationType == null || n.getNotificationType() == notificationType)
                .filter(n -> priority == null || n.getPriority() == priority)
                .filter(n -> isRead == null || n.getIsRead().equals(isRead))
                .collect(Collectors.toList());

        // Manual pagination
        int start = page * size;
        List<Notification> pagedNotifications = filtered.stream()
                .skip(start)
                .limit(size)
                .collect(Collectors.toList());

        // Count unread
        int unreadCount = (int) allNotifications.stream()
                .filter(n -> !n.getIsRead())
                .count();

        // Map to DTOs
        List<NotificationSummaryDTO> notificationDTOs = pagedNotifications.stream()
                .map(this::mapToNotificationSummaryDTO)
                .collect(Collectors.toList());

        return NotificationListResponseDTO.builder()
                .notifications(notificationDTOs)
                .totalElements(filtered.size())
                .totalPages((int) Math.ceil((double) filtered.size() / size))
                .currentPage(page)
                .pageSize(size)
                .unreadCount(unreadCount)
                .build();
    }

    /**
     * Marks a notification as read.
     * 
     * @param context ME_OFFICER context
     * @param notificationId Notification ID
     */
    @Transactional
    public void markNotificationAsRead(
            MEOfficerContext context,
            UUID notificationId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load notification
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found with ID: " + notificationId
                ));

        // Validate notification belongs to ME_OFFICER
        if (!notification.getRecipient().getId().equals(context.getMeOfficer().getId())) {
            throw new ResourceNotFoundException(
                    "Notification does not belong to you."
            );
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);

        log.info("ME_OFFICER {} marked notification {} as read", 
                context.getMeOfficer().getEmail(), notificationId);
    }

    /**
     * Marks multiple notifications as read.
     * 
     * @param context ME_OFFICER context
     * @param notificationIds List of notification IDs
     */
    @Transactional
    public void markNotificationsAsRead(
            MEOfficerContext context,
            List<UUID> notificationIds
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        for (UUID notificationId : notificationIds) {
            try {
                markNotificationAsRead(context, notificationId);
            } catch (Exception e) {
                log.error("Failed to mark notification {} as read: {}", notificationId, e.getMessage());
            }
        }
    }

    /**
     * Gets notification preferences for ME_OFFICER.
     * Note: Preferences may be stored in User model or a separate preferences table.
     * For now, returning default preferences.
     * 
     * @param context ME_OFFICER context
     * @return Notification preferences
     */
    public NotificationPreferencesDTO getNotificationPreferences(MEOfficerContext context) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // TODO: Load preferences from database if stored
        // For now, return default preferences
        return NotificationPreferencesDTO.builder()
                .emailEnabled(true)
                .pushEnabled(true)
                .smsEnabled(false)
                .alertNotifications(true)
                .reminderNotifications(true)
                .approvalRequestNotifications(true)
                .infoNotifications(true)
                .build();
    }

    /**
     * Updates notification preferences for ME_OFFICER.
     * 
     * @param context ME_OFFICER context
     * @param preferences Notification preferences
     * @return Updated preferences
     */
    @Transactional
    public NotificationPreferencesDTO updateNotificationPreferences(
            MEOfficerContext context,
            NotificationPreferencesDTO preferences
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // TODO: Save preferences to database if stored
        // For now, just return the preferences
        log.info("ME_OFFICER {} updated notification preferences", 
                context.getMeOfficer().getEmail());

        return preferences;
    }

    /**
     * Maps Notification entity to NotificationSummaryDTO.
     */
    private NotificationSummaryDTO mapToNotificationSummaryDTO(Notification notification) {
        return NotificationSummaryDTO.builder()
                .notificationId(notification.getId())
                .notificationType(notification.getNotificationType() != null ? 
                        notification.getNotificationType().name() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority() != null ? 
                        notification.getPriority().name() : null)
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
