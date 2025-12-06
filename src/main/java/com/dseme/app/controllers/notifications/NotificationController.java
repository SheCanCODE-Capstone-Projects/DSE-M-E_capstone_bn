package com.dseme.app.controllers.notifications;

import com.dseme.app.dtos.notifications.NotificaticationDTO;
import com.dseme.app.models.User;
import com.dseme.app.services.notifications.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{id}")
    public List<NotificaticationDTO> getNotificationsById(@PathVariable User id){
        return notificationService.getNotificationsById(id);
    }
}
