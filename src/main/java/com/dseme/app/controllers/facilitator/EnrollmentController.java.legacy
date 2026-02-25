package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.EnrollParticipantDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.models.Enrollment;
import com.dseme.app.services.facilitator.EnrollmentService;
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
@RestController
@RequestMapping("/api/facilitator/enrollments")
@RequiredArgsConstructor
public class EnrollmentController extends FacilitatorBaseController {

    private final EnrollmentService enrollmentService;

    /**
     * Enrolls a participant into the facilitator's active cohort.
     * 
     * POST /api/facilitator/enrollments
     * 
     * Rules:
     * - Participant must exist
     * - Participant must belong to facilitator's partner
     * - Participant must not already be enrolled in this cohort
     * - Cohort must be active (status = ACTIVE)
     * - Cohort must match facilitator's active cohort
     * - Cohort status must not be CANCELLED or COMPLETED
     * - Enrollment status is set to ENROLLED
     * 
     * Forbidden:
     * - Self-approval (verification flags cannot be set)
     * - Enrolling into past cohorts
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Enrollment data (participant ID)
     * @return Created enrollment
     */
    @PostMapping
    public ResponseEntity<Enrollment> enrollParticipant(
            HttpServletRequest request,
            @Valid @RequestBody EnrollParticipantDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Enrollment enrollment = enrollmentService.enrollParticipant(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }
}

