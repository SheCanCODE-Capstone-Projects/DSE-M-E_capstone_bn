package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentWithGradesDTO {
    private UUID id;
    private String title;
    private String description;
    private AssessmentType type;
    private String course;
    private String chapter;
    private LocalDate dueDate;
    private Integer maxScore;
    private Integer totalStudents;
    private Integer gradedStudents;
    private List<ParticipantGradeDTO> grades;
    private String createdByName;
    private Instant createdAt;
}
