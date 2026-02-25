package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.models.Attendance;
import com.dseme.app.services.facilitator.AttendanceService;
import com.dseme.app.services.facilitator.TodayAttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    private final TodayAttendanceService todayAttendanceService;

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

    /**
     * Gets today's attendance statistics for a training module.
     * 
     * GET /api/facilitator/attendance/today/stats?moduleId={moduleId}
     * 
     * Returns: PRESENT, ABSENT, LATE, EXCUSED counts and attendance rate percentage.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param moduleId Training module ID
     * @return Today's attendance statistics
     */
    @GetMapping("/today/stats")
    public ResponseEntity<TodayAttendanceStatsDTO> getTodayAttendanceStats(
            HttpServletRequest request,
            @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TodayAttendanceStatsDTO stats = todayAttendanceService.getTodayAttendanceStats(context, moduleId);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Gets today's attendance list for a training module.
     * 
     * GET /api/facilitator/attendance/today/list?moduleId={moduleId}
     * 
     * Returns: List of all participants with check-in time and attendance status.
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param moduleId Training module ID
     * @return List of today's attendance records
     */
    @GetMapping("/today/list")
    public ResponseEntity<List<TodayAttendanceListDTO>> getTodayAttendanceList(
            HttpServletRequest request,
            @RequestParam UUID moduleId
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        List<TodayAttendanceListDTO> list = todayAttendanceService.getTodayAttendanceList(context, moduleId);
        
        return ResponseEntity.ok(list);
    }

    /**
     * Records or updates today's attendance for a participant.
     * 
     * POST /api/facilitator/attendance/today/record
     * 
     * Handles:
     * - PRESENT button: Sets PRESENT (before threshold) or LATE (at/after threshold)
     * - ABSENT button: Sets ABSENT (no reason) or EXCUSED (with reason)
     * - Updates existing attendance if already recorded
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @param dto Attendance record DTO
     * @return Created or updated attendance record
     */
    @PostMapping("/today/record")
    public ResponseEntity<TodayAttendanceListDTO> recordTodayAttendance(
            HttpServletRequest request,
            @Valid @RequestBody RecordTodayAttendanceDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TodayAttendanceListDTO attendance = todayAttendanceService.recordTodayAttendance(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(attendance);
    }
}

