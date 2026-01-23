package com.dseme.app.dtos.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDTO {
    
    @NotBlank(message = "Requested role is required")
    private String requestedRole;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}