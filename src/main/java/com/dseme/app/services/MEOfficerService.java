package com.dseme.app.services;

import com.dseme.app.dtos.dashboard.MEOfficerDashboardDTO;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MEOfficerService {
    
    private final ParticipantRepository participantRepo;
    private final CohortRepository cohortRepo;
    private final AttendanceRepository attendanceRepo;
    private final SurveyRepository surveyRepo;
    
    public MEOfficerDashboardDTO getDashboard() {
        return MEOfficerDashboardDTO.builder()
            .stats(buildDashboardStats())
            .participants(getRecentParticipants())
            .cohorts(getActiveCohorts())
            .alerts(getSystemAlerts())
            .build();
    }
    
    private MEOfficerDashboardDTO.DashboardStats buildDashboardStats() {
        return MEOfficerDashboardDTO.DashboardStats.builder()
            .totalParticipants(participantRepo.count())
            .activeCohorts(3L) // Sample data
            .completionRate(calculateCompletionRate())
            .avgScore(calculateAverageScore())
            .employmentRate(calculateEmploymentRate())
            .pendingApprovals(0L) // M&E doesn't approve, just monitors
            .build();
    }
    
    private List<MEOfficerDashboardDTO.ParticipantOverview> getRecentParticipants() {
        // Return sample data for monitoring
        return Arrays.asList(
            MEOfficerDashboardDTO.ParticipantOverview.builder()
                .firstName("Sarah").lastName("Johnson")
                .cohortCode("A-001").status("ACTIVE")
                .averageScore(92.0).employmentStatus("EMPLOYED")
                .needsVerification(false)
                .build()
        );
    }
    
    private List<MEOfficerDashboardDTO.CohortOverview> getActiveCohorts() {
        return Arrays.asList(
            MEOfficerDashboardDTO.CohortOverview.builder()
                .cohortCode("A-001").cohortName("Digital Skills Cohort 1")
                .status("ACTIVE").participantCount(25L)
                .completionRate(85.0)
                .build()
        );
    }
    
    private List<MEOfficerDashboardDTO.AlertSummary> getSystemAlerts() {
        return Arrays.asList(
            MEOfficerDashboardDTO.AlertSummary.builder()
                .type("ATTENDANCE").message("Low attendance in Cohort B-002")
                .severity("MEDIUM").count(3L)
                .build()
        );
    }
    
    public Map<String, Object> getParticipants(int page, int size, String search, String cohort, String status) {
        // Return filtered participant data for monitoring
        return Map.of(
            "participants", getRecentParticipants(),
            "totalElements", 150L,
            "totalPages", 8
        );
    }
    
    public Map<String, Object> getAttendanceData(String cohort, String dateFrom, String dateTo) {
        // Return attendance analytics
        return Map.of(
            "attendanceRate", 87.5,
            "totalSessions", 45,
            "averageAttendance", 22.3
        );
    }
    
    public Map<String, Object> getAssessmentData(String cohort, String assessmentType) {
        // Return assessment analytics
        return Map.of(
            "averageScore", 78.3,
            "passRate", 92.0,
            "improvementRate", 15.2
        );
    }
    
    public Map<String, Object> getSurveyData(String surveyType) {
        // Return survey analytics
        return Map.of(
            "responseRate", 82.0,
            "satisfactionScore", 4.2,
            "completedSurveys", 124
        );
    }
    
    public Map<String, Object> getOutcomeData(String cohort) {
        // Return employment and outcome data
        return Map.of(
            "employmentRate", 76.5,
            "internshipRate", 45.2,
            "businessStarted", 12
        );
    }
    
    public Map<String, Object> exportReport(String reportType, String format) {
        // Return export data
        return Map.of(
            "reportUrl", "/reports/" + reportType + "." + format,
            "generatedAt", new Date(),
            "status", "READY"
        );
    }
    
    private Double calculateCompletionRate() {
        return 76.5; // Sample calculation
    }
    
    private Double calculateAverageScore() {
        return 78.3; // Sample calculation
    }
    
    private Double calculateEmploymentRate() {
        return 65.2; // Sample calculation
    }
}