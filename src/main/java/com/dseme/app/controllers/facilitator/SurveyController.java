package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.models.Survey;
import com.dseme.app.services.facilitator.SurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Survey Management", description = "APIs for managing surveys and survey responses")
@RestController
@RequestMapping("/api/facilitator/surveys")
@RequiredArgsConstructor
public class SurveyController extends FacilitatorBaseController {

    private final SurveyService surveyService;
    private final com.dseme.app.services.facilitator.SurveyStatsService surveyStatsService;

    /**
     * Sends a survey to participants in the facilitator's active cohort.
     * 
     * POST /api/facilitator/surveys/send
     */
    @Operation(
        summary = "Send survey",
        description = "Sends a survey to participants in the facilitator's active cohort. " +
                     "One survey per type per participant is enforced."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Survey sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
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
     * Gets complete survey detail including summary, questions, and paginated participant responses.
     * 
     * GET /api/facilitator/surveys/{surveyId}/detail
     */
    @Operation(
        summary = "Get survey detail",
        description = "Retrieves complete survey detail including summary, questions, and paginated participant response statuses."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Survey detail retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{surveyId}/detail")
    public ResponseEntity<SurveyDetailResponseDTO> getSurveyDetail(
            HttpServletRequest request,
            @Parameter(description = "Survey ID") @PathVariable UUID surveyId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        SurveyDetailResponseDTO detail = surveyService.getSurveyDetail(context, surveyId, pageable);
        
        return ResponseEntity.ok(detail);
    }

    /**
     * Gets all survey responses for a specific survey.
     * FACILITATOR can only view responses for surveys in their active cohort.
     * 
     * GET /api/facilitator/surveys/{surveyId}/responses
     */
    @Operation(
        summary = "Get survey responses",
        description = "Retrieves all survey responses for a specific survey. Only responses from facilitator's active cohort are returned."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Responses retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{surveyId}/responses")
    public ResponseEntity<List<SurveyResponseDTO>> getSurveyResponses(
            HttpServletRequest request,
            @Parameter(description = "Survey ID") @PathVariable UUID surveyId
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
     */
    @Operation(
        summary = "Get all cohort survey responses",
        description = "Retrieves all survey responses for facilitator's active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Responses retrieved successfully")
    })
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
     */
    @Operation(
        summary = "Get survey response by ID",
        description = "Retrieves a specific survey response by ID. Only responses from facilitator's active cohort are accessible."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Response retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/responses/{responseId}")
    public ResponseEntity<SurveyResponseDTO> getSurveyResponseById(
            HttpServletRequest request,
            @Parameter(description = "Response ID") @PathVariable UUID responseId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        SurveyResponseDTO response = surveyService.getSurveyResponseById(context, responseId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets survey statistics for facilitator's active cohort.
     * 
     * GET /api/facilitator/surveys/stats
     */
    @Operation(
        summary = "Get survey statistics",
        description = "Retrieves survey statistics including active/completed surveys count, average response rate, and pending responses."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public ResponseEntity<SurveyStatsDTO> getSurveyStats(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        SurveyStatsDTO stats = surveyStatsService.getSurveyStats(context);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets survey overview (list of survey cards) for facilitator's active cohort.
     * 
     * GET /api/facilitator/surveys/overview
     */
    @Operation(
        summary = "Get survey overview",
        description = "Retrieves survey overview with cards showing status, response rate, completion progress, and action flags."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Overview retrieved successfully")
    })
    @GetMapping("/overview")
    public ResponseEntity<SurveyOverviewResponseDTO> getSurveyOverview(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        SurveyOverviewResponseDTO overview = surveyService.getSurveyOverview(context);
        
        return ResponseEntity.ok(overview);
    }

    /**
     * Gets pending responses (Action Required) for facilitator's active cohort.
     * 
     * GET /api/facilitator/surveys/pending-responses
     */
    @Operation(
        summary = "Get pending responses",
        description = "Retrieves list of participants who haven't responded to active surveys, including days remaining."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pending responses retrieved successfully")
    })
    @GetMapping("/pending-responses")
    public ResponseEntity<PendingResponsesResponseDTO> getPendingResponses(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        PendingResponsesResponseDTO pendingResponses = surveyService.getPendingResponses(context);
        
        return ResponseEntity.ok(pendingResponses);
    }

    /**
     * Sends reminders to participants with pending survey responses.
     * 
     * POST /api/facilitator/surveys/send-reminders
     */
    @Operation(
        summary = "Send survey reminders",
        description = "Sends reminders to participants with pending survey responses. Currently logs the action (email integration pending)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reminders sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/send-reminders")
    public ResponseEntity<String> sendReminders(
            HttpServletRequest request,
            @Valid @RequestBody SendRemindersDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        String result = surveyService.sendReminders(context, dto);
        
        return ResponseEntity.ok(result);
    }
}

