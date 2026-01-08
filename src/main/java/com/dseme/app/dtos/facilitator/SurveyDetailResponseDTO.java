package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO for complete survey detail response.
 * Aggregates survey summary, questions, and paginated participant responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDetailResponseDTO {
    /**
     * Survey detail header/summary with KPIs.
     */
    private SurveyDetailDTO surveyDetail;
    
    /**
     * List of survey questions.
     */
    private List<QuestionDTO> questions;
    
    /**
     * Paginated list of participant response statuses.
     */
    private Page<ParticipantStatusDTO> participantResponses;
}

