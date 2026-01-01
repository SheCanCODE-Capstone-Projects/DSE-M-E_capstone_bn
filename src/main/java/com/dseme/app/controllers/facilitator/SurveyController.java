package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.SendSurveyDTO;
import com.dseme.app.dtos.facilitator.SurveyResponseDTO;
import com.dseme.app.models.Survey;
import com.dseme.app.services.facilitator.SurveyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for survey management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 * 
 * Role-based access:
 * - FACILITATOR: Can send surveys, view responses only for their cohort
 */
@RestController
@RequestMapping("/api/facilitator/surveys")
@RequiredArgsConstructor
public class SurveyController extends FacilitatorBaseController {

    private final SurveyService surveyService;

    /**
     * Sends a survey to participants in the facilitator's active cohort.
     * 
     * POST /api/facilitator/surveys/send
     * 
     * Rules:
     * - Participant must be in active cohort
     * - One survey per type per participant (enforced by unique constraint)
     * - Survey is associated with facilitator's active cohort
     * 
     * Survey Types:
     * - BASELINE
     * - MIDLINE
     * - ENDLINE
     * - TRACER
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Survey data
     * @return Created survey
     */
    @PostMapping("/send")
    public ResponseEntity<Survey> sendSurvey(
            HttpServletRequest request,
            @Valid @RequestBody SendSurveyDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Survey survey = surveyService.sendSurvey(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(survey);
    }

    /**
     * Gets all survey responses for a specific survey.
     * FACILITATOR can only view responses for surveys in their active cohort.
     * 
     * GET /api/facilitator/surveys/{surveyId}/responses
     * 
     * Rules:
     * - Survey must belong to facilitator's active cohort
     * - Only responses from facilitator's active cohort are returned
     * - Cannot see responses from other cohorts
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param surveyId Survey ID
     * @return List of survey responses
     */
    @GetMapping("/{surveyId}/responses")
    public ResponseEntity<List<SurveyResponseDTO>> getSurveyResponses(
            HttpServletRequest request,
            @PathVariable UUID surveyId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<SurveyResponseDTO> responses = surveyService.getSurveyResponses(context, surveyId);
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Gets all survey responses for facilitator's active cohort.
     * FACILITATOR can only view responses for their cohort.
     * 
     * GET /api/facilitator/surveys/responses
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @return List of survey responses for facilitator's active cohort
     */
    @GetMapping("/responses")
    public ResponseEntity<List<SurveyResponseDTO>> getAllCohortSurveyResponses(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<SurveyResponseDTO> responses = surveyService.getAllCohortSurveyResponses(context);
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Gets a specific survey response by ID.
     * FACILITATOR can only view responses for their active cohort.
     * 
     * GET /api/facilitator/surveys/responses/{responseId}
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param responseId Response ID
     * @return Survey response
     */
    @GetMapping("/responses/{responseId}")
    public ResponseEntity<SurveyResponseDTO> getSurveyResponseById(
            HttpServletRequest request,
            @PathVariable UUID responseId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        SurveyResponseDTO response = surveyService.getSurveyResponseById(context, responseId);
        
        return ResponseEntity.ok(response);
    }
}

