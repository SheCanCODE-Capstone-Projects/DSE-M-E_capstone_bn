package com.dseme.app.dtos.me;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCohortDTO {
    @NotBlank(message = "Cohort name is required")
    private String name;
    
    @NotNull(message = "Course ID is required")
    private UUID courseId;
    
    private UUID facilitatorId;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @Min(value = 1, message = "Max participants must be at least 1")
    private Integer maxParticipants;
}