package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for updating facilitator profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFacilitatorRequestDTO {
    private String firstName;
    private String lastName;
    private UUID centerId; // Optional - reassign to different center
}
