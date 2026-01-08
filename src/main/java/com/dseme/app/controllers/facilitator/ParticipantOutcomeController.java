package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.services.facilitator.ParticipantOutcomeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for participant outcomes management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@RestController
@RequestMapping("/api/facilitator/outcomes")
@RequiredArgsConstructor
public class ParticipantOutcomeController extends FacilitatorBaseController {

    private final ParticipantOutcomeService participantOutcomeService;

    /**
     * Gets outcome statistics for facilitator's active cohort.
     * 
     * GET /api/facilitator/outcomes/stats
     * 
     * Returns:
     * - Employed count
     * - Internship count
     * - In training count
     * - Success rate percentage
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @return Outcome statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<OutcomeStatsDTO> getOutcomeStats(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        OutcomeStatsDTO stats = participantOutcomeService.getOutcomeStats(context);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets all participant outcomes for facilitator's active cohort.
     * 
     * GET /api/facilitator/outcomes
     * 
     * Returns list of participant outcome records with employment status, company, position, etc.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @return List of participant outcomes
     */
    @GetMapping
    public ResponseEntity<List<ParticipantOutcomeDTO>> getParticipantOutcomes(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<ParticipantOutcomeDTO> outcomes = participantOutcomeService.getParticipantOutcomes(context);
        
        return ResponseEntity.ok(outcomes);
    }

    /**
     * Updates or creates a participant outcome.
     * 
     * POST /api/facilitator/outcomes
     * PUT /api/facilitator/outcomes
     * 
     * Creates a new outcome or updates existing one for a participant.
     * 
     * Validation:
     * - Company name and position required if status is EMPLOYED or INTERNSHIP
     * - Employment type required if status is EMPLOYED or INTERNSHIP
     * - TRAINING status allows null/empty company name and position
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Update outcome request
     * @return Updated participant outcome
     */
    @PostMapping
    public ResponseEntity<ParticipantOutcomeDTO> updateOutcome(
            HttpServletRequest request,
            @Valid @RequestBody UpdateOutcomeRequestDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantOutcomeDTO outcome = participantOutcomeService.updateOutcome(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(outcome);
    }

    /**
     * Updates an existing participant outcome.
     * 
     * PUT /api/facilitator/outcomes/{outcomeId}
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param outcomeId Outcome ID
     * @param dto Update outcome request
     * @return Updated participant outcome
     */
    @PutMapping("/{outcomeId}")
    public ResponseEntity<ParticipantOutcomeDTO> updateOutcomeById(
            HttpServletRequest request,
            @PathVariable UUID outcomeId,
            @Valid @RequestBody UpdateOutcomeRequestDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        // For now, use the same update logic (can be enhanced to update specific outcome by ID)
        ParticipantOutcomeDTO outcome = participantOutcomeService.updateOutcome(context, dto);
        
        return ResponseEntity.ok(outcome);
    }
}

