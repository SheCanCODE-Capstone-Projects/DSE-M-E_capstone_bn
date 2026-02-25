package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.services.facilitator.AttendanceMarkingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/facilitator/attendance")
@RequiredArgsConstructor
public class AttendanceController extends FacilitatorBaseController {

    private final AttendanceMarkingService attendanceMarkingService;

    @GetMapping("/participants")
    public ResponseEntity<AttendanceParticipantsResponseDTO> getParticipantsForAttendance(
            HttpServletRequest request,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        LocalDate sessionDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : sessionDate;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : sessionDate;
        
        AttendanceParticipantsResponseDTO response = attendanceMarkingService
                .getParticipantsForAttendance(context, cohortId, start, end);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mark")
    public ResponseEntity<MarkAttendanceResponseDTO> markAttendance(
            HttpServletRequest request,
            @Valid @RequestBody MarkAttendanceRequestDTO requestBody
    ) {
        FacilitatorContext context = getFacilitatorContext(request);
        MarkAttendanceResponseDTO response = attendanceMarkingService.markAttendance(context, requestBody);
        return ResponseEntity.ok(response);
    }
}
