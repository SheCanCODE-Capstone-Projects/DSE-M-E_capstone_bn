package com.dseme.app.dtos.me;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignOrganizationDTO {
    @NotBlank(message = "Partner ID is required")
    private String partnerId;
    
    @NotNull(message = "Center ID is required")
    private UUID centerId;
}
