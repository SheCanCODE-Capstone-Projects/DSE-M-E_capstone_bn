package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AssessmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssignmentDTO {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Assessment type is required")
    private AssessmentType type;

    @NotNull(message = "Module ID is required")
    private UUID moduleId;

    private LocalDate dueDate;

    @NotNull(message = "Max score is required")
    @Min(value = 1, message = "Max score must be at least 1")
    private Integer maxScore;
}
