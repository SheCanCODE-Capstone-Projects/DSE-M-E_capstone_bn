package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for viewing survey responses.
 * Contains response information without exposing sensitive data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResponseDTO {
    private UUID responseId;
    private UUID surveyId;
    private String surveyTitle;
    private String surveyType;
    private UUID participantId;
    private String participantName;
    private String participantEmail;
    private Instant submittedAt;
    private UUID submittedBy;
    private List<SurveyAnswerDTO> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurveyAnswerDTO {
        private UUID answerId;
        private UUID questionId;
        private String questionText;
        private String answerValue;
    }
}

