package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.SendNotificationDTO;
import com.dseme.app.dtos.notifications.NotificationDTO;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Notification;
import com.dseme.app.models.Participant;
import com.dseme.app.models.User;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.NotificationRepository;
import com.dseme.app.repositories.ParticipantRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for facilitator notification management.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FacilitatorNotificationService {

    private final NotificationRepository notificationRepository;
    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Sends notifications to participants.
     */
    public void sendNotificationsToParticipants(FacilitatorContext context, SendNotificationDTO dto) {
        cohortIsolationService.getFacilitatorActiveCohort(context);

        for (UUID participantId : dto.getParticipantIds()) {
            Participant participant = participantRepository.findById(participantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

            // Validate participant belongs to facilitator's cohort
            enrollmentRepository.findByParticipantIdAndCohortId(
                    participantId, context.getCohortId())
                    .orElseThrow(() -> new AccessDeniedException(
                        "Access denied. Participant is not enrolled in your active cohort."
                    ));

            // Get participant's user account (if exists)
            User participantUser = userRepository.findByEmail(participant.getEmail()).orElse(null);
            if (participantUser == null) {
                // Participant doesn't have a user account yet, skip notification
                continue;
            }

            Notification notification = new Notification();
            notification.setRecipient(participantUser);
            notification.setTitle(dto.getTitle());
            notification.setMessage(dto.getMessage());
            notification.setNotificationType(NotificationType.INFO);
            notification.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
            notification.setIsRead(false);
            notification.setCreatedAt(Instant.now());

            notificationRepository.save(notification);
        }
    }

    /**
     * Gets all notifications for the facilitator.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getFacilitatorNotifications(FacilitatorContext context) {
        User facilitator = context.getFacilitator();

        return notificationRepository.findByRecipient(facilitator).stream()
                .map(notification -> new NotificationDTO(
                        facilitator.getEmail(),
                        notification.getNotificationType().toString(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getIsRead(),
                        notification.getPriority().toString()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Marks a notification as read.
     */
    public void markNotificationAsRead(UUID notificationId, FacilitatorContext context) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Validate notification belongs to facilitator
        if (!notification.getRecipient().getId().equals(context.getFacilitator().getId())) {
            throw new AccessDeniedException("Access denied. Notification does not belong to you.");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}

