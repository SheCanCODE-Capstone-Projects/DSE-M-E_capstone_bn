package com.dseme.app.controllers;

import com.dseme.app.dtos.dashboard.MEOfficerDashboardDTO;
import com.dseme.app.services.MEOfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me-officer")
@RequiredArgsConstructor
public class MEOfficerController {
    
    private final MEOfficerService meOfficerService;
    
    @GetMapping("/dashboard")
    public ResponseEntity<MEOfficerDashboardDTO> getDashboard() {
        return ResponseEntity.ok(meOfficerService.getDashboard());
    }
    
    @GetMapping("/participants")
    public ResponseEntity<?> getParticipants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cohort,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(meOfficerService.getParticipants(page, size, search, cohort, status));
    }
    
    @GetMapping("/attendance")
    public ResponseEntity<?> getAttendanceData(
            @RequestParam(required = false) String cohort,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return ResponseEntity.ok(meOfficerService.getAttendanceData(cohort, dateFrom, dateTo));
    }
    
    @GetMapping("/assessments")
    public ResponseEntity<?> getAssessmentData(
            @RequestParam(required = false) String cohort,
            @RequestParam(required = false) String assessmentType) {
        return ResponseEntity.ok(meOfficerService.getAssessmentData(cohort, assessmentType));
    }
    
    @GetMapping("/surveys")
    public ResponseEntity<?> getSurveyData(
            @RequestParam(required = false) String surveyType) {
        return ResponseEntity.ok(meOfficerService.getSurveyData(surveyType));
    }
    
    @GetMapping("/outcomes")
    public ResponseEntity<?> getOutcomeData(
            @RequestParam(required = false) String cohort) {
        return ResponseEntity.ok(meOfficerService.getOutcomeData(cohort));
    }
    
    @GetMapping("/reports/export")
    public ResponseEntity<?> exportReport(
            @RequestParam String reportType,
            @RequestParam String format) {
        return ResponseEntity.ok(meOfficerService.exportReport(reportType, format));
    }
}