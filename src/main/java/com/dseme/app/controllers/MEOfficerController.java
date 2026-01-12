package com.dseme.app.controllers;

import com.dseme.app.dtos.dashboard.MEOfficerDashboardDTO;
import com.dseme.app.services.MEOfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me-officer")
@RequiredArgsConstructor
public class MEOfficerController {
    
    private final MEOfficerService meOfficerService;
    
    @GetMapping("/dashboard")
    public ResponseEntity<MEOfficerDashboardDTO> getDashboard() {
        return ResponseEntity.ok(meOfficerService.getDashboard());
    }
    
    // Participants Management
    @GetMapping("/participants")
    public ResponseEntity<?> getParticipants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cohort,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String employmentStatus) {
        return ResponseEntity.ok(meOfficerService.getParticipants(page, size, search, cohort, gender, employmentStatus));
    }
    
    @PostMapping("/participants")
    public ResponseEntity<?> addParticipant(@RequestBody Object participantData) {
        return ResponseEntity.ok(meOfficerService.addParticipant(participantData));
    }
    
    @GetMapping("/participants/export")
    public ResponseEntity<?> exportParticipants(
            @RequestParam String format,
            @RequestParam(required = false) String cohort,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String employmentStatus) {
        return ResponseEntity.ok(meOfficerService.exportParticipants(format, cohort, gender, employmentStatus));
    }
    
    @GetMapping("/participants/stats")
    public ResponseEntity<?> getParticipantStats() {
        return ResponseEntity.ok(meOfficerService.getParticipantStats());
    }
    
    // Facilitators Management
    @GetMapping("/facilitators")
    public ResponseEntity<?> getFacilitators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(meOfficerService.getFacilitators(page, size, search));
    }
    
    @GetMapping("/facilitators/requests")
    public ResponseEntity<?> getFacilitatorRequests() {
        return ResponseEntity.ok(meOfficerService.getFacilitatorRequests());
    }
    
    @PostMapping("/facilitators")
    public ResponseEntity<?> addFacilitator(@RequestBody Object facilitatorData) {
        return ResponseEntity.ok(meOfficerService.addFacilitator(facilitatorData));
    }
    
    @PostMapping("/facilitators/{id}/assign-cohort")
    public ResponseEntity<?> assignCohortToFacilitator(
            @PathVariable Long id,
            @RequestBody Object cohortAssignment) {
        return ResponseEntity.ok(meOfficerService.assignCohortToFacilitator(id, cohortAssignment));
    }
    
    @PostMapping("/facilitators/{id}/assign-course")
    public ResponseEntity<?> assignCourseToFacilitator(
            @PathVariable Long id,
            @RequestBody Object courseAssignment) {
        return ResponseEntity.ok(meOfficerService.assignCourseToFacilitator(id, courseAssignment));
    }
    
    // Surveys Management
    @GetMapping("/surveys")
    public ResponseEntity<?> getSurveys() {
        return ResponseEntity.ok(meOfficerService.getSurveys());
    }
    
    @PostMapping("/surveys")
    public ResponseEntity<?> createSurvey(@RequestBody Object surveyData) {
        return ResponseEntity.ok(meOfficerService.createSurvey(surveyData));
    }
    
    @GetMapping("/surveys/{id}/responses")
    public ResponseEntity<?> getSurveyResponses(@PathVariable Long id) {
        return ResponseEntity.ok(meOfficerService.getSurveyResponses(id));
    }
    
    @PostMapping("/surveys/{id}/send")
    public ResponseEntity<?> sendSurvey(@PathVariable Long id) {
        return ResponseEntity.ok(meOfficerService.sendSurvey(id));
    }
    
    // Reports & Alerts
    @GetMapping("/reports")
    public ResponseEntity<?> getReports() {
        return ResponseEntity.ok(meOfficerService.getReports());
    }
    
    @GetMapping("/reports/{id}/download")
    public ResponseEntity<?> downloadReport(@PathVariable Long id) {
        return ResponseEntity.ok(meOfficerService.downloadReport(id));
    }
    
    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        return ResponseEntity.ok(meOfficerService.getAlerts());
    }
    
    @PostMapping("/alerts/{id}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(meOfficerService.resolveAlert(id));
    }
    
    // Dashboard Analytics
    @GetMapping("/analytics/monthly-progress")
    public ResponseEntity<?> getMonthlyProgress() {
        return ResponseEntity.ok(meOfficerService.getMonthlyProgress());
    }
    
    @GetMapping("/analytics/program-distribution")
    public ResponseEntity<?> getProgramDistribution() {
        return ResponseEntity.ok(meOfficerService.getProgramDistribution());
    }
    
    @GetMapping("/analytics/top-facilitators")
    public ResponseEntity<?> getTopFacilitators() {
        return ResponseEntity.ok(meOfficerService.getTopFacilitators());
    }
    
    @GetMapping("/analytics/cohort-status")
    public ResponseEntity<?> getCohortStatus() {
        return ResponseEntity.ok(meOfficerService.getCohortStatus());
    }
    
    @GetMapping("/analytics/course-distribution")
    public ResponseEntity<?> getCourseDistribution() {
        return ResponseEntity.ok(meOfficerService.getCourseDistribution());
    }
    
    // Legacy endpoints for backward compatibility
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