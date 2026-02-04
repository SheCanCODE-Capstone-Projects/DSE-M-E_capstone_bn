package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.NotificationListResponseDTO;
import com.dseme.app.dtos.donor.NotificationSummaryDTO;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.enums.Role;
import com.dseme.app.models.Notification;
import com.dseme.app.models.User;
import com.dseme.app.repositories.NotificationRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for DONOR notification management.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DonorNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Gets all notifications for DONOR users with pagination and filtering.
     * 
     * @param context DONOR context
     * @param page Page number (0-based)
     * @param size Page size
     * @param notificationType Optional filter by notification type
     * @param priority Optional filter by priority
     * @param isRead Optional filter by read status
     * @return Paginated notification list
     */
    @Transactional(readOnly = true)
    public NotificationListResponseDTO getNotifications(
            DonorContext context,
            int page,
            int size,
            NotificationType notificationType,
            Priority priority,
            Boolean isRead
    ) {
        // Get all DONOR users
        List<User> donorUsers = userRepository.findByRole(Role.DONOR);

        // Build pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Get all notifications for DONOR users
        List<Notification> allNotifications = donorUsers.stream()
                .flatMap(user -> notificationRepository.findByRecipient(user).stream())
                .collect(Collectors.toList());

        // Apply filters
        List<Notification> filtered = allNotifications.stream()
                .filter(n -> notificationType == null || n.getNotificationType() == notificationType)
                .filter(n -> priority == null || n.getPriority() == priority)
                .filter(n -> isRead == null || Boolean.TRUE.equals(n.getIsRead()) == isRead)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        // Apply pagination manually
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Notification> pagedNotifications = start < filtered.size() ?
                filtered.subList(start, end) : List.of();

        // Map to DTOs
        List<NotificationSummaryDTO> notificationDTOs = pagedNotifications.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        // Count unread
        long unreadCount = filtered.stream()
                .filter(n -> Boolean.FALSE.equals(n.getIsRead()))
                .count();

        int totalPages = (int) Math.ceil((double) filtered.size() / size);

        return NotificationListResponseDTO.builder()
                .notifications(notificationDTOs)
                .totalCount((long) filtered.size())
                .unreadCount(unreadCount)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Marks a notification as read.
     * 
     * @param context DONOR context
     * @param notificationId Notification ID
     */
    public void markAsRead(DonorContext context, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                    "Notification with ID '" + notificationId + "' not found."
                ));

        // Verify notification belongs to a DONOR user
        if (notification.getRecipient().getRole() != Role.DONOR) {
            throw new com.dseme.app.exceptions.AccessDeniedException(
                "Access denied. This notification does not belong to a DONOR user."
            );
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks all notifications as read for DONOR users.
     * 
     * @param context DONOR context
     */
    public void markAllAsRead(DonorContext context) {
        List<User> donorUsers = userRepository.findByRole(Role.DONOR);
        
        for (User donorUser : donorUsers) {
            List<Notification> notifications = notificationRepository.findByRecipient(donorUser);
            for (Notification notification : notifications) {
                if (Boolean.FALSE.equals(notification.getIsRead())) {
                    notification.setIsRead(true);
                    notificationRepository.save(notification);
                }
            }
        }
    }

    /**
     * Maps Notification entity to NotificationSummaryDTO.
     */
    private NotificationSummaryDTO mapToSummaryDTO(Notification notification) {
        return NotificationSummaryDTO.builder()
                .id(notification.getId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .priority(notification.getPriority())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
