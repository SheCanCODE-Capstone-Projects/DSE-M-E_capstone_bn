package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.UploadScoreDTO;
import com.dseme.app.models.Score;
import com.dseme.app.services.facilitator.ScoreService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for score management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@RestController
@RequestMapping("/api/facilitator/scores")
@RequiredArgsConstructor
public class ScoreController extends FacilitatorBaseController {

    private final ScoreService scoreService;

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
}

