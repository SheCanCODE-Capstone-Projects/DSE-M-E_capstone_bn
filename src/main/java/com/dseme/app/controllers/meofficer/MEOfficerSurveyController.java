package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.SendSurveyRequestDTO;
import com.dseme.app.dtos.meofficer.SurveySummaryRequestDTO;
import com.dseme.app.dtos.meofficer.SurveySummaryResponseDTO;
import com.dseme.app.models.Survey;
import com.dseme.app.services.meofficer.MEOfficerSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
