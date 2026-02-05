package com.dseme.app.dtos.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for changing user email address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailRequestDTO {

    @NotBlank(message = "New email is required")
    @Email(message = "Email must be valid")
    private String newEmail;
}
