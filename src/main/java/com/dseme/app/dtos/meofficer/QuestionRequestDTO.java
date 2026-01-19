package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a survey question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDTO {
    /**
     * Question text.
     */
    @NotBlank(message = "Question text is required")
    private String questionText;
    
    /**
     * Question type.
     */
    @NotNull(message = "Question type is required")
    private QuestionType questionType;
    
    /**
     * Whether question is required.
     */
    @Builder.Default
    private Boolean isRequired = false;
    
    /**
     * Sequence order (for ordering questions).
     */
    @NotNull(message = "Sequence order is required")
    private Integer sequenceOrder;
}
