package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AssessmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for uploading scores (single or batch).
 * 
 * For single score: provide one entry in the list.
 * For batch scores: provide multiple entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadScoreDTO {

    @NotNull(message = "Score records are required")
    @Valid
    private List<ScoreRecord> records;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreRecord {
        @NotNull(message = "Enrollment ID is required")
        private UUID enrollmentId;

        @NotNull(message = "Module ID is required")
        private UUID moduleId;

        @NotNull(message = "Assessment type is required")
        private AssessmentType assessmentType;

        /**
         * Name of the assessment (e.g., "Midterm Exam", "Project 1", "Quiz 3").
         * Optional but recommended for better tracking.
         */
        private String assessmentName;

        @NotNull(message = "Score value is required")
        @DecimalMin(value = "0.0", message = "Score must be at least 0")
        @DecimalMax(value = "100.0", message = "Score must be at most 100")
        private BigDecimal scoreValue;

        /**
         * Maximum possible score for this assessment.
         * Optional, defaults to 100.0 if not provided.
         */
        private java.math.BigDecimal maxScore;

        /**
         * Date when the assessment was conducted.
         * Optional, but recommended for consistency.
         * If not provided, will use current date.
         */
        private java.time.LocalDate assessmentDate;
    }
}

