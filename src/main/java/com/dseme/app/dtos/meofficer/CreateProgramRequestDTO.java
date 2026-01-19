package com.dseme.app.dtos.meofficer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new program.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProgramRequestDTO {
    @NotBlank(message = "Program name is required")
    private String programName;

    private String description;

    @NotNull(message = "Duration in weeks is required")
    @Positive(message = "Duration must be positive")
    private Integer durationWeeks;

    @Builder.Default
    private Boolean isActive = true;
}
