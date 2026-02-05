package com.dseme.app.dtos.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSettingsRequestDTO {

    private Boolean emailNotificationsEnabled;
    private Boolean accountChangeNotifications;
    private Boolean systemUpdateNotifications;
}
