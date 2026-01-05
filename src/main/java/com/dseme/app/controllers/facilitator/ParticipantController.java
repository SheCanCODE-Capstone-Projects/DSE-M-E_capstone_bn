package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.models.Enrollment;
import com.dseme.app.services.facilitator.ParticipantListService;
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
    private final ParticipantListService participantListService;

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

    /**
     * Gets detailed participant information by ID.
     * 
     * GET /api/facilitator/participants/{participantId}/detail
     * 
     * Returns: firstName, lastName, email, phone, gender, disabilityStatus, 
     *          cohortName, enrollmentStatus, attendancePercentage
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param participantId Participant ID
     * @return Participant detail
     */
    @GetMapping("/{participantId}/detail")
    public ResponseEntity<ParticipantDetailDTO> getParticipantDetail(
            HttpServletRequest request,
            @PathVariable UUID participantId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantDetailDTO participant = participantService.getParticipantDetail(context, participantId);
        
        return ResponseEntity.ok(participant);
    }

    /**
     * Gets paginated list of participants with search, filter, and sort.
     * 
     * GET /api/facilitator/participants/list
     * 
     * Query Parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - search: Search term (name, email, phone)
     * - sortBy: Sort field (firstName, lastName, email, phone, enrollmentDate, attendancePercentage, enrollmentStatus)
     * - sortDirection: Sort direction (ASC, DESC)
     * - enrollmentStatusFilter: Filter by status (ACTIVE, INACTIVE, COMPLETED, DROPPED_OUT, WITHDRAWN)
     * - genderFilter: Filter by gender (MALE, FEMALE, OTHER)
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param listRequest List request parameters
     * @return Paginated participant list
     */
    @GetMapping("/list")
    public ResponseEntity<ParticipantListResponseDTO> getAllParticipants(
            HttpServletRequest request,
            @ModelAttribute ParticipantListRequestDTO listRequest
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantListResponseDTO response = participantListService.getAllParticipants(context, listRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets participant statistics (active/inactive counts, gender distribution).
     * 
     * GET /api/facilitator/participants/statistics
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @return Participant statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ParticipantStatisticsDTO> getParticipantStatistics(
            HttpServletRequest request
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        ParticipantStatisticsDTO statistics = participantListService.getParticipantStatistics(context);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Updates enrollment status manually (DROPPED_OUT, WITHDRAWN).
     * 
     * PUT /api/facilitator/participants/enrollments/{enrollmentId}/status
     * 
     * Rules:
     * - Only DROPPED_OUT and WITHDRAWN can be set by facilitator
     * - Enrollment must belong to facilitator's active cohort
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param enrollmentId Enrollment ID
     * @param dto Status update DTO
     * @return Updated enrollment
     */
    @PutMapping("/enrollments/{enrollmentId}/status")
    public ResponseEntity<Enrollment> updateEnrollmentStatus(
            HttpServletRequest request,
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody UpdateEnrollmentStatusDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Enrollment enrollment = participantService.updateEnrollmentStatus(context, enrollmentId, dto);
        
        return ResponseEntity.ok(enrollment);
    }
}

