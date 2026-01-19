package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a program.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgramRequestDTO {
    private String programName;
    private String description;
    
    @Positive(message = "Duration must be positive")
    private Integer durationWeeks;
    
    private Boolean isActive;
}
