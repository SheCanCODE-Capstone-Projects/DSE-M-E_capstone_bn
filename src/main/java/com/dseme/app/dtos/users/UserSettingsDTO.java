package com.dseme.app.dtos.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user account settings and preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsDTO {

    /**
     * Email notification preferences.
     */
    @Builder.Default
    private Boolean emailNotificationsEnabled = true;

    /**
     * Receive notifications for account changes.
     */
    @Builder.Default
    private Boolean accountChangeNotifications = true;

    /**
     * Receive notifications for system updates.
     */
    @Builder.Default
    private Boolean systemUpdateNotifications = false;
}
