package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.GradeResponseDTO;
import com.dseme.app.dtos.facilitator.UploadScoreDTO;
import com.dseme.app.services.facilitator.ScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/facilitator/grades")
@RequiredArgsConstructor
public class GradeController extends FacilitatorBaseController {

    private final ScoreService scoreService;

    /**
     * Upload scores for participants (single or batch)
     * POST /api/facilitator/grades
     */
    @PostMapping
    public ResponseEntity<List<GradeResponseDTO>> uploadScores(
            @Valid @RequestBody UploadScoreDTO dto,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        List<GradeResponseDTO> scores = scoreService.uploadScores(context, dto);
        return ResponseEntity.ok(scores);
    }

    /**
     * Get all scores for a specific participant
     * GET /api/facilitator/grades/participant/{participantId}
     */
    @GetMapping("/participant/{participantId}")
    public ResponseEntity<List<GradeResponseDTO>> getParticipantScores(
            @PathVariable UUID participantId,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(scoreService.getParticipantScoresDTO(participantId, context));
    }

    /**
     * Get all scores for a specific module
     * GET /api/facilitator/grades/module/{moduleId}
     */
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<List<GradeResponseDTO>> getModuleScores(
            @PathVariable UUID moduleId,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(scoreService.getModuleScoresDTO(moduleId, context));
    }

    /**
     * Get scores for a specific assignment
     * GET /api/facilitator/grades/assignment/{assignmentId}
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<GradeResponseDTO>> getAssignmentScores(
            @PathVariable UUID assignmentId,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(scoreService.getAssignmentScoresDTO(assignmentId, context));
    }
}
