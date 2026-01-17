package com.dseme.app.dtos.me;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignCourseDTO {
    @NotNull(message = "Course ID is required")
    private UUID courseId;
}