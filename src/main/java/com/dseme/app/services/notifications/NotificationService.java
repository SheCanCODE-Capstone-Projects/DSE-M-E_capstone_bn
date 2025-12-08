package com.dseme.app.services.notifications;

import com.dseme.app.dtos.notifications.NotificationDTO;
import com.dseme.app.enums.NotificationType;
import com.dseme.app.enums.Priority;
import com.dseme.app.models.Notification;
import com.dseme.app.models.RoleRequest;
import com.dseme.app.models.User;
import com.dseme.app.repositories.*;
import com.dseme.app.services.users.UserPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final UserPermissionService userPermissionService;

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo,  UserPermissionService userPermissionService) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.userPermissionService = userPermissionService;
    }

    // getting a list of notifications associated with a user
    public List<NotificationDTO> getNotificationsById (HttpServletRequest request){
        User user = userPermissionService.getActor(request);

        //Check if user and notification recipient are one and the same and load the notification
        return notificationRepo.findByRecipient(user)
                .stream()
                .filter(notify -> Boolean.FALSE.equals(notify.getIsRead()))
                .map(notification ->
                        new NotificationDTO(
                                user.getEmail(),
                                notification.getNotificationType().toString(),
                                notification.getTitle(),
                                notification.getMessage(),
                                notification.getIsRead(),
                                notification.getPriority().toString()))
                .collect(Collectors.toList());
    }

    // sending an approval_request notification
    public void sendApprovalNotification(User approver, RoleRequest requester, String title, String message) {
        Notification notification = new Notification();

        notification.setTitle(title);
        notification.setRecipient(
                userRepo.findById(approver.getId()).get()
        );
        notification.setMessage(message);
        notification.setNotificationType(NotificationType.APPROVAL_REQUEST);
        notification.setIsRead(false);
        notification.setPriority(Priority.HIGH);
        notification.setRoleRequest(requester);

        notificationRepo.save(notification);
    }

    // sending an info notification
    public void sendInfoNotification(User requester, String title, String message, Priority priority) {

        Notification notify = new Notification();

        notify.setTitle(title);
        notify.setMessage(message);
        notify.setRecipient(requester);
        notify.setCreatedAt(Instant.now());
        notify.setNotificationType(NotificationType.INFO);
        notify.setPriority(priority);
        notify.setIsRead(false);
        notificationRepo.save(notify);
    }

    // marking a notification as read so that it won't warrant attention
    public void markNotificationAsRead(UUID requestId) {

        List<Notification> notificationProcessed = notificationRepo.findByRoleRequestId(requestId);

        for(Notification notification : notificationProcessed){
            notification.setIsRead(true);
            notificationRepo.save(notification);
        }
    }
}
