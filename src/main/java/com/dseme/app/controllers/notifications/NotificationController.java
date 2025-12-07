package com.dseme.app.controllers.notifications;

import com.dseme.app.dtos.notifications.NotificaticationDTO;
import com.dseme.app.models.User;
import com.dseme.app.services.notifications.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping()
    public List<NotificaticationDTO> getNotificationsById(HttpServletRequest request){
        return notificationService.getNotificationsById(request);
    }
}
