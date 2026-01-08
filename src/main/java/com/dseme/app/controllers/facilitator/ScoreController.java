package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.models.Score;
import com.dseme.app.services.facilitator.GradeTrackingService;
import com.dseme.app.services.facilitator.ScoreService;
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
 * Controller for score management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Grade/Score Management", description = "APIs for managing grades and scores")
@RestController
@RequestMapping("/api/facilitator/scores")
@RequiredArgsConstructor
public class ScoreController extends FacilitatorBaseController {

    private final ScoreService scoreService;
    private final GradeTrackingService gradeTrackingService;

    /**
     * Uploads scores for one or more participants (batch support).
     * 
     * POST /api/facilitator/scores
     * 
     * Rules:
     * - Participant must be enrolled
     * - Participant must belong to active cohort
     * - Score value must be between 0 and 100
     * 
     * Non-Functional:
     * - Numeric validation (BigDecimal)
     * - Score ranges enforced (0-100)
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Score data (single or batch)
     * @return List of created score records
     */
    @PostMapping
    public ResponseEntity<List<Score>> uploadScores(
            HttpServletRequest request,
            @Valid @RequestBody UploadScoreDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<Score> scores = scoreService.uploadScores(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(scores);
    }

    /**
     * Gets grade statistics for a training module.
     * 
     * GET /api/facilitator/scores/stats?moduleId={moduleId}
     * 
     * Returns: Class average, high performers count, need attention count, total assessments.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param moduleId Training module ID
     * @return Grade statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<GradeStatsDTO> getGradeStats(
            HttpServletRequest request,
            @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        GradeStatsDTO stats = gradeTrackingService.getGradeStats(context, moduleId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets list of high performers (overall average >= 80%).
     * 
     * GET /api/facilitator/scores/high-performers?moduleId={moduleId}
     * 
     * Returns: List of participants with overall grade >= 80%, sorted by grade descending.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param moduleId Training module ID
     * @return List of high performers
     */
    @GetMapping("/high-performers")
    public ResponseEntity<List<ParticipantGradeSummaryDTO>> getHighPerformers(
            HttpServletRequest request,
            @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<ParticipantGradeSummaryDTO> highPerformers = gradeTrackingService.getHighPerformers(context, moduleId);
        
        return ResponseEntity.ok(highPerformers);
    }

    /**
     * Gets list of participants needing attention (overall average <= 60%).
     * 
     * GET /api/facilitator/scores/need-attention?moduleId={moduleId}
     * 
     * Returns: List of participants with overall grade <= 60%, sorted by grade descending.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param moduleId Training module ID
     * @return List of participants needing attention
     */
    @GetMapping("/need-attention")
    public ResponseEntity<List<ParticipantGradeSummaryDTO>> getNeedAttention(
            HttpServletRequest request,
            @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<ParticipantGradeSummaryDTO> needAttention = gradeTrackingService.getNeedAttention(context, moduleId);
        
        return ResponseEntity.ok(needAttention);
    }

    /**
     * Searches participants by name and returns their grade summary.
     * 
     * GET /api/facilitator/scores/search?moduleId={moduleId}&name={name}
     * 
     * Returns: List of matching participants with grade summaries.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param moduleId Training module ID
     * @param name Search term (participant name)
     * @return List of matching participants
     */
    @GetMapping("/search")
    public ResponseEntity<List<ParticipantGradeSummaryDTO>> searchParticipantsByName(
            HttpServletRequest request,
            @RequestParam UUID moduleId,
            @RequestParam String name
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<ParticipantGradeSummaryDTO> results = gradeTrackingService.searchParticipantsByName(
                context, moduleId, name);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Gets detailed grade information for a specific participant.
     * 
     * GET /api/facilitator/scores/participants/{enrollmentId}/detail?moduleId={moduleId}
     * 
     * Returns: Participant details with all assessments and scores.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param enrollmentId Enrollment ID
     * @param moduleId Training module ID
     * @return Detailed participant grade information
     */
    @GetMapping("/participants/{enrollmentId}/detail")
    public ResponseEntity<ParticipantGradeDetailDTO> getParticipantGradeDetail(
            HttpServletRequest request,
            @PathVariable UUID enrollmentId,
            @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantGradeDetailDTO detail = gradeTrackingService.getParticipantGradeDetail(
                context, enrollmentId, moduleId);
        
        return ResponseEntity.ok(detail);
    }
}

