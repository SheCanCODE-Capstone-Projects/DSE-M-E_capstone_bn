package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.BulkEnrollmentDTO;
import com.dseme.app.dtos.facilitator.BulkEnrollmentResponseDTO;
import com.dseme.app.dtos.facilitator.EnrollParticipantDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.models.Enrollment;
import com.dseme.app.services.facilitator.EnrollmentService;
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

/**
 * Controller for enrollment management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Enrollment Management", description = "APIs for managing participant enrollments")
@RestController
@RequestMapping("/api/facilitator/enrollments")
@RequiredArgsConstructor
public class EnrollmentController extends FacilitatorBaseController {

    private final EnrollmentService enrollmentService;

    /**
     * Enrolls a participant into the facilitator's active cohort.
     * 
     * POST /api/facilitator/enrollments
     */
    @Operation(
        summary = "Enroll participant",
        description = "Enrolls a single participant into the facilitator's active cohort."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Participant enrolled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "409", description = "Participant already enrolled")
    })
    @PostMapping
    public ResponseEntity<Enrollment> enrollParticipant(
            HttpServletRequest request,
            @Valid @RequestBody EnrollParticipantDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Enrollment enrollment = enrollmentService.enrollParticipant(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    /**
     * Bulk enrolls multiple participants into the facilitator's active cohort.
     * 
     * POST /api/facilitator/enrollments/bulk
     */
    @Operation(
        summary = "Bulk enroll participants",
        description = "Enrolls multiple participants at once. Returns success/failure counts and error details."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bulk enrollment completed"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkEnrollmentResponseDTO> bulkEnrollParticipants(
            HttpServletRequest request,
            @Valid @RequestBody BulkEnrollmentDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        BulkEnrollmentResponseDTO response = enrollmentService.bulkEnrollParticipants(
                context, dto.getParticipantIds());
        
        return ResponseEntity.ok(response);
    }
}

