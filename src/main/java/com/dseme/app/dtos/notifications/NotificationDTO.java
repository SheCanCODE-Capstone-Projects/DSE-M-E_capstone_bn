package com.dseme.app.dtos.notifications;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {

    private String recipient;
    private String notificationType;
    private String subject;
    private String body;
    private Boolean isRead;
    private String priority;

}
