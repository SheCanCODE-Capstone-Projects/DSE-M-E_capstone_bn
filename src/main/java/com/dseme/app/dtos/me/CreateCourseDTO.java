package com.dseme.app.dtos.me;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseDTO {
    @NotBlank(message = "Course name is required")
    private String name;
    
    @NotBlank(message = "Course code is required")
    private String code;
    
    private String description;
    
    @NotBlank(message = "Course level is required")
    private String level;
    
    @Min(value = 1, message = "Duration must be at least 1 week")
    private Integer durationWeeks;
    
    @Min(value = 1, message = "Max participants must be at least 1")
    private Integer maxParticipants;
}