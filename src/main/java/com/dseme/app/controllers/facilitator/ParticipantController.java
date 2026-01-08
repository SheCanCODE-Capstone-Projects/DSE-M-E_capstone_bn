package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.models.Enrollment;
import com.dseme.app.services.facilitator.ParticipantListService;
import com.dseme.app.services.facilitator.ParticipantService;
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

import java.util.UUID;

/**
 * Controller for participant management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Participant Management", description = "APIs for managing participants")
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
     */
    @Operation(
        summary = "Create participant",
        description = "Creates a new participant profile and automatically enrolls them in the facilitator's active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Participant created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Participant already exists")
    })
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
     */
    @Operation(
        summary = "Update participant",
        description = "Updates participant profile. Only editable fields can be updated (firstName, lastName, gender, disabilityStatus, dateOfBirth)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participant updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Participant not found")
    })
    @PutMapping("/{participantId}")
    public ResponseEntity<ParticipantResponseDTO> updateParticipant(
            HttpServletRequest request,
            @Parameter(description = "Participant ID") @PathVariable UUID participantId,
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
     */
    @Operation(
        summary = "Get participant by ID",
        description = "Retrieves a participant by ID. Participant must belong to facilitator's active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participant retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Participant not found")
    })
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
     */
    @Operation(
        summary = "Get participant detail",
        description = "Retrieves detailed participant information including cohort name, enrollment status, and attendance percentage."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participant detail retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Participant not found")
    })
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
     */
    @Operation(
        summary = "Get participant list",
        description = "Retrieves paginated list of participants with search, filter, and sort capabilities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Participant list retrieved successfully")
    })
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
     */
    @Operation(
        summary = "Get participant statistics",
        description = "Retrieves participant statistics including active/inactive counts and gender distribution."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
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
     */
    @Operation(
        summary = "Update enrollment status",
        description = "Manually updates enrollment status. Only DROPPED_OUT and WITHDRAWN can be set by facilitator."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Enrollment status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @PutMapping("/enrollments/{enrollmentId}/status")
    public ResponseEntity<Enrollment> updateEnrollmentStatus(
            HttpServletRequest request,
            @Parameter(description = "Enrollment ID") @PathVariable UUID enrollmentId,
            @Valid @RequestBody UpdateEnrollmentStatusDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Enrollment enrollment = participantService.updateEnrollmentStatus(context, enrollmentId, dto);
        
        return ResponseEntity.ok(enrollment);
    }
}

