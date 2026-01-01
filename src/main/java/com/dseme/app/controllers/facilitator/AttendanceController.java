package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.RecordAttendanceDTO;
import com.dseme.app.models.Attendance;
import com.dseme.app.services.facilitator.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for attendance management by facilitators.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@RestController
@RequestMapping("/api/facilitator/attendance")
@RequiredArgsConstructor
public class AttendanceController extends FacilitatorBaseController {

    private final AttendanceService attendanceService;

    /**
     * Records attendance for one or more participants (batch support).
     * 
     * POST /api/facilitator/attendance
     * 
     * Rules:
     * - Participant must be enrolled
     * - Participant must belong to active cohort
     * - One attendance per enrollment per module per date
     * 
     * Constraints:
     * - Duplicate → 409 CONFLICT (handled by idempotency - returns existing record)
     * 
     * Non-Functional:
     * - Idempotency enforced (duplicate requests return existing record)
     * - Batch attendance allowed
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Attendance data (single or batch)
     * @return List of created or existing attendance records
     */
    @PostMapping
    public ResponseEntity<List<Attendance>> recordAttendance(
            HttpServletRequest request,
            @Valid @RequestBody RecordAttendanceDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<Attendance> attendances = attendanceService.recordAttendance(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(attendances);
    }
}

