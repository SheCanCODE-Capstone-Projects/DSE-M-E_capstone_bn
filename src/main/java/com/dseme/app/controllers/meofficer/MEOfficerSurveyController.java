package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.models.Survey;
import com.dseme.app.services.meofficer.MEOfficerSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for ME_OFFICER survey operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Only surveys and participants belonging to ME_OFFICER's assigned partner are accessible.
 */
@Tag(name = "ME Officer Surveys", description = "Survey management endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/surveys")
@RequiredArgsConstructor
public class MEOfficerSurveyController extends MEOfficerBaseController {

    private final MEOfficerSurveyService surveyService;

    /**
     * Sends a survey to partner participants.
     * Only partner participants can receive surveys.
     * Survey type is enforced via enum (BASELINE, MIDLINE, ENDLINE, TRACER).
     * 
     * POST /api/me-officer/surveys/send
     */
    @Operation(
            summary = "Send survey",
            description = "Sends a survey to partner participants. " +
                    "Only participants belonging to ME_OFFICER's partner can receive surveys. " +
                    "Survey type is enforced via enum. " +
                    "Optional cohort filtering is supported."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Survey sent successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or participant already has survey of this type"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or participant/cohort does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Participant or cohort not found")
    })
    @PostMapping("/send")
    public ResponseEntity<Survey> sendSurvey(
            HttpServletRequest request,
            @Valid @RequestBody SendSurveyRequestDTO sendRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        Survey survey = surveyService.sendSurvey(context, sendRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(survey);
    }

    /**
     * Gets survey analytics summary.
     * Returns aggregated responses only - no raw PII exposed.
     * 
     * GET /api/me-officer/surveys/summary
     * 
     * Query Parameters:
     * - surveyId: Survey ID (required)
     * - cohortId: Optional cohort ID to filter responses
     */
    @Operation(
            summary = "Get survey summary",
            description = "Retrieves aggregated survey analytics. " +
                    "Returns aggregated responses only - no raw PII (personally identifiable information) exposed. " +
                    "Only surveys belonging to ME_OFFICER's partner are accessible. " +
                    "Supports optional cohort filtering."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Survey summary retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or survey does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Survey not found")
    })
    @GetMapping("/summary")
    public ResponseEntity<SurveySummaryResponseDTO> getSurveySummary(
            HttpServletRequest request,
            @RequestParam UUID surveyId,
            @RequestParam(required = false) UUID cohortId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        SurveySummaryRequestDTO summaryRequest = SurveySummaryRequestDTO.builder()
                .surveyId(surveyId)
                .cohortId(cohortId)
                .build();
        
        SurveySummaryResponseDTO response = surveyService.getSurveySummary(context, summaryRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets overview of all surveys for the partner.
     * Returns program-wide survey overview for dashboard grid view.
     * 
     * GET /api/me-officer/surveys/overview
     */
    @Operation(
            summary = "Get all surveys overview",
            description = "Retrieves overview of all surveys for the ME_OFFICER's partner. " +
                    "Returns program-wide survey overview with identification, scope, status, " +
                    "and aggregated completion rates. Suitable for dashboard grid view."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Survey overview retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/overview")
    public ResponseEntity<List<GlobalSurveyOverviewDTO>> getAllSurveysOverview(
            HttpServletRequest request
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        List<GlobalSurveyOverviewDTO> overviews = surveyService.getAllSurveysOverview(context);
        
        return ResponseEntity.ok(overviews);
    }

    /**
     * Gets detailed survey view with analytics.
     * Includes question schema, response analytics, trend data, and cohort breakdown.
     * 
     * GET /api/me-officer/surveys/{surveyId}/detail
     */
    @Operation(
            summary = "Get survey detail with analytics",
            description = "Retrieves detailed survey view including question schema, " +
                    "response analytics (aggregated, no PII), trend data (responses over time), " +
                    "and cohort breakdown showing which cohorts are lagging in completion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Survey detail retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Survey does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Survey not found")
    })
    @GetMapping("/{surveyId}/detail")
    public ResponseEntity<SurveyDetailDTO> getSurveyDetail(
            HttpServletRequest request,
            @Parameter(description = "Survey ID") @PathVariable UUID surveyId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        SurveyDetailDTO detail = surveyService.getSurveyDetail(context, surveyId);
        
        return ResponseEntity.ok(detail);
    }

    /**
     * Creates a new survey with questions.
     * Supports targeting ALL participants or SPECIFIC_COHORTS.
     * 
     * POST /api/me-officer/surveys/create
     */
    @Operation(
            summary = "Create new survey",
            description = "Creates a new survey with questions. " +
                    "Supports targeting ALL participants in the partner or SPECIFIC_COHORTS. " +
                    "If targeting SPECIFIC_COHORTS, creates one survey per cohort. " +
                    "Survey starts in DRAFT status and can be published later."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Survey created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or cohort does not belong to your partner")
    })
    @PostMapping("/create")
    public ResponseEntity<Survey> createSurvey(
            HttpServletRequest request,
            @Valid @RequestBody CreateSurveyRequestDTO createRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        Survey survey = surveyService.createSurvey(context, createRequest);
        
        return ResponseEntity.status(201).body(survey);
    }

    /**
     * Triggers bulk reminders for non-responders.
     * Only works for surveys in PENDING or ACTIVE status.
     * 
     * POST /api/me-officer/surveys/remind
     */
    @Operation(
            summary = "Trigger bulk reminders",
            description = "Triggers bulk reminders for all non-responders to a survey. " +
                    "Only works for surveys in PENDING or ACTIVE status. " +
                    "Sends reminders to all participants who have not yet submitted their responses."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reminders triggered successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Survey is in COMPLETED status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Survey does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Survey not found")
    })
    @PostMapping("/remind")
    public ResponseEntity<BulkReminderResponseDTO> triggerBulkReminder(
            HttpServletRequest request,
            @Valid @RequestBody BulkReminderRequestDTO reminderRequest
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        BulkReminderResponseDTO response = surveyService.triggerBulkReminder(context, reminderRequest);
        
        return ResponseEntity.ok(response);
    }
}
