package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for survey question.
 * Represents a single question in a survey.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    /**
     * Question ID.
     */
    private UUID questionId;
    
    /**
     * Question number (e.g., Q1, Q2).
     * Derived from sequenceOrder.
     */
    private Integer questionNumber;
    
    /**
     * The actual question text being asked.
     */
    private String questionText;
    
    /**
     * Question type.
     * Values: TEXT, NUMBER, SINGLE_CHOICE, MULTIPLE_CHOICE, SCALE
     * Note: YES_NO questions can be represented as SINGLE_CHOICE.
     */
    private String questionType;
    
    /**
     * Whether the question is required.
     * Used for validation on frontend/backend.
     */
    private Boolean isRequired;
    
    /**
     * Sequence order of the question in the survey.
     */
    private Integer sequenceOrder;
}
