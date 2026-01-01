package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.CreateParticipantDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.ParticipantResponseDTO;
import com.dseme.app.dtos.facilitator.UpdateParticipantDTO;
import com.dseme.app.models.Participant;
import com.dseme.app.services.facilitator.ParticipantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for participant management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@RestController
@RequestMapping("/api/facilitator/participants")
@RequiredArgsConstructor
public class ParticipantController extends FacilitatorBaseController {

    private final ParticipantService participantService;

    /**
     * Creates a new participant profile and enrolls them in the facilitator's active cohort.
     * 
     * POST /api/facilitator/participants
     * 
     * Rules:
     * - Participant must not already exist (by email)
     * - Participant must not have any existing enrollment
     * - Participant is assigned to facilitator's partner
     * - Participant is enrolled in facilitator's active cohort
     * - Enrollment status is set to ENROLLED
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Participant creation data
     * @return Created participant
     */
    @PostMapping
    public ResponseEntity<ParticipantResponseDTO> createParticipant(
            HttpServletRequest request,
            @Valid @RequestBody CreateParticipantDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantResponseDTO participant = participantService.createParticipant(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }

    /**
     * Updates a participant profile.
     * 
     * PUT /api/facilitator/participants/{participantId}
     * 
     * Rules:
     * - Participant must exist
     * - Participant must belong to facilitator's cohort
     * - Participant must belong to facilitator's center
     * - Only editable fields can be updated: firstName, lastName, gender, disabilityStatus, dateOfBirth
     * - Immutable fields: partner, cohort, status, verification flags
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param participantId Participant ID to update
     * @param dto Update data (only editable fields)
     * @return Updated participant
     */
    @PutMapping("/{participantId}")
    public ResponseEntity<ParticipantResponseDTO> updateParticipant(
            HttpServletRequest request,
            @PathVariable UUID participantId,
            @Valid @RequestBody UpdateParticipantDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);

        ParticipantResponseDTO participant = participantService.updateParticipant(context, participantId, dto);
        
        return ResponseEntity.ok(participant);
    }

    /**
     * Gets a participant by ID.
     * 
     * GET /api/facilitator/participants/{participantId}
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param participantId Participant ID
     * @return Participant
     */
    @GetMapping("/{participantId}")
    public ResponseEntity<ParticipantResponseDTO> getParticipant(
            HttpServletRequest request,
            @PathVariable UUID participantId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);

        ParticipantResponseDTO participant = participantService.getParticipantById(context, participantId);
        
        return ResponseEntity.ok(participant);
    }
}

