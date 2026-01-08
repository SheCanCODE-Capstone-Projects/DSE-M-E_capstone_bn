package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.models.Attendance;
import com.dseme.app.services.facilitator.AttendanceService;
import com.dseme.app.services.facilitator.TodayAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
@Tag(name = "Attendance Management", description = "APIs for managing attendance records")
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
     */
    @Operation(
        summary = "Record attendance",
        description = "Records attendance for one or more participants. Supports batch operations and idempotency."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attendance recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
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
     */
    @Operation(
        summary = "Get today's attendance statistics",
        description = "Retrieves today's attendance statistics including PRESENT, ABSENT, LATE, EXCUSED counts and attendance rate."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
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
     */
    @Operation(
        summary = "Get today's attendance list",
        description = "Retrieves list of all participants with check-in time and attendance status for today."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attendance list retrieved successfully")
    })
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
     */
    @Operation(
        summary = "Record today's attendance",
        description = "Records or updates today's attendance. PRESENT button sets PRESENT/LATE based on threshold. ABSENT button sets ABSENT/EXCUSED based on reason."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attendance recorded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/today/record")
    public ResponseEntity<TodayAttendanceListDTO> recordTodayAttendance(
            HttpServletRequest request,
            @Valid @RequestBody RecordTodayAttendanceDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        TodayAttendanceListDTO attendance = todayAttendanceService.recordTodayAttendance(context, dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(attendance);
    }

    /**
     * Gets historical attendance records for a date range.
     * 
     * GET /api/facilitator/attendance/history?moduleId={moduleId}&startDate={startDate}&endDate={endDate}
     */
    @Operation(
        summary = "Get historical attendance",
        description = "Retrieves historical attendance records for a specific module within a date range."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historical attendance retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/history")
    public ResponseEntity<HistoricalAttendanceDTO> getHistoricalAttendance(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @RequestParam UUID moduleId,
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        HistoricalAttendanceDTO historical = attendanceService.getHistoricalAttendance(
                context, moduleId, startDate, endDate);
        
        return ResponseEntity.ok(historical);
    }

    /**
     * Updates an existing attendance record.
     * 
     * PUT /api/facilitator/attendance/{attendanceId}
     */
    @Operation(
        summary = "Update attendance record",
        description = "Updates an existing attendance record. Used for corrections."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Attendance updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Attendance record not found")
    })
    @PutMapping("/{attendanceId}")
    public ResponseEntity<Attendance> updateAttendance(
            HttpServletRequest request,
            @Parameter(description = "Attendance record ID") @PathVariable UUID attendanceId,
            @Valid @RequestBody UpdateAttendanceDTO dto
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        
        Attendance attendance = attendanceService.updateAttendance(context, attendanceId, dto);
        
        return ResponseEntity.ok(attendance);
    }
}

