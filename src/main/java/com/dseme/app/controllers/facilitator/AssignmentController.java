package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.services.facilitator.AssignmentService;
import com.dseme.app.services.facilitator.AssignmentGradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/facilitator/assignments")
@RequiredArgsConstructor
public class AssignmentController extends FacilitatorBaseController {

    private final AssignmentService assignmentService;
    private final AssignmentGradingService assignmentGradingService;

    @PostMapping
    public ResponseEntity<AssignmentResponseDTO> createAssignment(
            @Valid @RequestBody CreateAssignmentDTO dto,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(assignmentService.createAssignment(context, dto));
    }

    @GetMapping
    public ResponseEntity<List<AssignmentResponseDTO>> getCohortAssignments(
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(assignmentService.getCohortAssignments(context));
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponseDTO> getAssignment(
            @PathVariable UUID assignmentId,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(assignmentService.getAssignment(context, assignmentId));
    }
    
    @GetMapping("/{assignmentId}/grades")
    public ResponseEntity<AssignmentWithGradesDTO> getAssignmentWithGrades(
            @PathVariable UUID assignmentId,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(assignmentGradingService.getAssignmentWithGrades(context, assignmentId));
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponseDTO> updateAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CreateAssignmentDTO dto,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(assignmentService.updateAssignment(context, assignmentId, dto));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable UUID assignmentId,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        assignmentService.deleteAssignment(context, assignmentId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/grade")
    public ResponseEntity<List<ParticipantGradeDTO>> batchGradeParticipants(
            @Valid @RequestBody BatchGradeRequest request,
            @RequestAttribute("facilitatorContext") FacilitatorContext context
    ) {
        return ResponseEntity.ok(assignmentGradingService.batchGradeParticipants(context, request));
    }
}
