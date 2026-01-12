package com.dseme.app.services;

import com.dseme.app.dtos.dashboard.MEOfficerDashboardDTO;
import com.dseme.app.enums.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MEOfficerService {
    
    private final ParticipantRepository participantRepo;
    private final CohortRepository cohortRepo;
    private final AttendanceRepository attendanceRepo;
    private final SurveyRepository surveyRepo;
    private final UserRepository userRepo;
    private final ScoreRepository scoreRepo;
    private final RoleRequestRepository roleRequestRepo;
    
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
            .activeCohorts(cohortRepo.countByStatus(CohortStatus.ACTIVE))
            .completionRate(calculateCompletionRate())
            .avgScore(calculateAverageScore())
            .employmentRate(calculateEmploymentRate())
            .activeFacilitators(userRepo.countByRoleAndIsActiveTrue(Role.FACILITATOR))
            .totalCohorts(cohortRepo.count())
            .courseCoverage(calculateCourseCoverage())
            .pendingAccessRequests(roleRequestRepo.countByStatus(RequestStatus.PENDING))
            .surveyResponseRate(calculateSurveyResponseRate())
            .build();
    }
    
    // Participants Management
    public Map<String, Object> getParticipants(int page, int size, String search, String cohort, String gender, String employmentStatus) {
        List<Map<String, Object>> participants = Arrays.asList(
            Map.of(
                "id", 1L,
                "name", "Sarah Johnson",
                "cohort", "Cohort 2024-Q1",
                "gender", "FEMALE",
                "employment", "EMPLOYED",
                "score", 85.5,
                "annualIncome", 45000,
                "status", "ACTIVE",
                "actions", Arrays.asList("view", "edit", "export")
            ),
            Map.of(
                "id", 2L,
                "name", "Michael Chen",
                "cohort", "Cohort 2024-Q1",
                "gender", "MALE",
                "employment", "UNEMPLOYED",
                "score", 78.2,
                "annualIncome", 0,
                "status", "ACTIVE",
                "actions", Arrays.asList("view", "edit", "export")
            )
        );
        
        return Map.of(
            "participants", participants,
            "totalElements", 150L,
            "totalPages", (int) Math.ceil(150.0 / size),
            "currentPage", page,
            "size", size
        );
    }
    
    public Map<String, Object> addParticipant(Object participantData) {
        return Map.of(
            "success", true,
            "message", "Participant added successfully",
            "participantId", 123L
        );
    }
    
    public Map<String, Object> exportParticipants(String format, String cohort, String gender, String employmentStatus) {
        return Map.of(
            "downloadUrl", "/api/me-officer/participants/download/" + UUID.randomUUID(),
            "format", format.toUpperCase(),
            "generatedAt", LocalDateTime.now(),
            "totalRecords", 150
        );
    }
    
    public Map<String, Object> getParticipantStats() {
        return Map.of(
            "totalParticipants", 150L,
            "topPerformers", 25L,
            "avgScore", 78.3,
            "activeCohorts", 5L
        );
    }
    
    // Facilitators Management
    public Map<String, Object> getFacilitators(int page, int size, String search) {
        List<Map<String, Object>> facilitators = Arrays.asList(
            Map.of(
                "id", 1L,
                "name", "Patricia Lee",
                "email", "patricia@example.com",
                "region", "West Region",
                "participants", 41,
                "assignedCohorts", Arrays.asList(
                    Map.of("id", 1L, "name", "Cohort 2024-Q1")
                ),
                "teaches", Arrays.asList(
                    Map.of("id", 1L, "name", "Leadership Dev")
                )
            )
        );
        
        return Map.of(
            "facilitators", facilitators,
            "totalElements", 25L,
            "totalPages", (int) Math.ceil(25.0 / size),
            "currentPage", page
        );
    }
    
    public Map<String, Object> getFacilitatorRequests() {
        List<Map<String, Object>> requests = Arrays.asList(
            Map.of(
                "id", 1L,
                "name", "John Smith",
                "email", "john@example.com",
                "requestedRole", "FACILITATOR",
                "requestDate", LocalDateTime.now().minusDays(2),
                "status", "PENDING"
            )
        );
        
        return Map.of(
            "requests", requests,
            "totalPending", 3L
        );
    }
    
    public Map<String, Object> addFacilitator(Object facilitatorData) {
        return Map.of(
            "success", true,
            "message", "Facilitator added successfully",
            "facilitatorId", 456L
        );
    }
    
    public Map<String, Object> assignCohortToFacilitator(Long facilitatorId, Object cohortAssignment) {
        return Map.of(
            "success", true,
            "message", "Cohort assigned successfully"
        );
    }
    
    public Map<String, Object> assignCourseToFacilitator(Long facilitatorId, Object courseAssignment) {
        return Map.of(
            "success", true,
            "message", "Course assigned successfully"
        );
    }
    
    // Surveys Management
    public Map<String, Object> getSurveys() {
        List<Map<String, Object>> surveys = Arrays.asList(
            Map.of(
                "id", 1L,
                "name", "Baseline Survey",
                "status", "ACTIVE",
                "completionRate", calculateSurveyCompletionRate(1L),
                "actions", Arrays.asList("view")
            ),
            Map.of(
                "id", 2L,
                "name", "Mid-term Evaluation",
                "status", "ACTIVE",
                "completionRate", calculateSurveyCompletionRate(2L),
                "actions", Arrays.asList("view")
            ),
            Map.of(
                "id", 3L,
                "name", "Endline Survey",
                "status", "PENDING",
                "completionRate", calculateSurveyCompletionRate(3L),
                "actions", Arrays.asList("send")
            ),
            Map.of(
                "id", 4L,
                "name", "Feedback",
                "status", "COMPLETED",
                "completionRate", calculateSurveyCompletionRate(4L),
                "actions", Arrays.asList("view")
            )
        );
        
        return Map.of(
            "surveys", surveys,
            "totalSurveys", surveys.size()
        );
    }
    
    public Map<String, Object> createSurvey(Object surveyData) {
        return Map.of(
            "success", true,
            "message", "Survey created successfully",
            "surveyId", 789L
        );
    }
    
    public Map<String, Object> getSurveyResponses(Long surveyId) {
        return Map.of(
            "surveyId", surveyId,
            "totalResponses", 124L,
            "responseRate", 82.7,
            "averageScore", 4.2
        );
    }
    
    public Map<String, Object> sendSurvey(Long surveyId) {
        return Map.of(
            "success", true,
            "message", "Survey sent successfully",
            "recipientCount", 150
        );
    }
    
    // Reports & Alerts
    public Map<String, Object> getReports() {
        List<Map<String, Object>> reports = Arrays.asList(
            Map.of(
                "id", 1L,
                "name", "Monthly Progress Report",
                "type", "MONTHLY",
                "size", "2.4 MB",
                "generatedAt", LocalDateTime.now().minusDays(1),
                "status", "READY"
            ),
            Map.of(
                "id", 2L,
                "name", "Quarterly Performance",
                "type", "QUARTERLY",
                "size", "3.1 MB",
                "generatedAt", LocalDateTime.now().minusDays(7),
                "status", "READY"
            ),
            Map.of(
                "id", 3L,
                "name", "Annual Summary",
                "type", "ANNUAL",
                "size", "5.2 MB",
                "generatedAt", LocalDateTime.now().minusDays(30),
                "status", "READY"
            )
        );
        
        return Map.of(
            "reports", reports,
            "totalReports", reports.size()
        );
    }
    
    public Map<String, Object> downloadReport(Long reportId) {
        return Map.of(
            "downloadUrl", "/api/me-officer/reports/download/" + reportId,
            "expiresAt", LocalDateTime.now().plusHours(1)
        );
    }
    
    public Map<String, Object> getAlerts() {
        List<Map<String, Object>> alerts = Arrays.asList(
            Map.of(
                "id", 1L,
                "type", "ATTENDANCE",
                "title", "Missing Attendance Records",
                "description", "5 issues found",
                "severity", "HIGH",
                "count", 5,
                "action", "Review Now"
            ),
            Map.of(
                "id", 2L,
                "type", "COMPLETION",
                "title", "Low Completion Rate",
                "description", "3 issues found",
                "severity", "MEDIUM",
                "count", 3,
                "action", "Investigate"
            ),
            Map.of(
                "id", 3L,
                "type", "SURVEY",
                "title", "New Survey Available",
                "description", "1 issues found",
                "severity", "LOW",
                "count", 1,
                "action", "Send"
            )
        );
        
        return Map.of(
            "alerts", alerts,
            "totalAlerts", alerts.size()
        );
    }
    
    public Map<String, Object> resolveAlert(Long alertId) {
        return Map.of(
            "success", true,
            "message", "Alert resolved successfully"
        );
    }
    
    // Dashboard Analytics
    public Map<String, Object> getMonthlyProgress() {
        List<Map<String, Object>> monthlyData = Arrays.asList(
            Map.of("month", "Jan", "completed", 45, "inProgress", 25, "notStarted", 10),
            Map.of("month", "Feb", "completed", 52, "inProgress", 28, "notStarted", 8),
            Map.of("month", "Mar", "completed", 48, "inProgress", 32, "notStarted", 12)
        );
        
        return Map.of(
            "monthlyProgress", monthlyData,
            "trend", "IMPROVING"
        );
    }
    
    public Map<String, Object> getProgramDistribution() {
        return Map.of(
            "trainingCompleted", 65,
            "inProgress", 25,
            "notStarted", 10
        );
    }
    
    public Map<String, Object> getTopFacilitators() {
        List<Map<String, Object>> topFacilitators = Arrays.asList(
            Map.of("name", "Patricia Lee", "score", 95.2, "participants", 41),
            Map.of("name", "Michael Johnson", "score", 92.8, "participants", 38),
            Map.of("name", "Sarah Wilson", "score", 90.5, "participants", 35)
        );
        
        return Map.of(
            "topFacilitators", topFacilitators
        );
    }
    
    public Map<String, Object> getCohortStatus() {
        List<Map<String, Object>> cohortStatus = Arrays.asList(
            Map.of("status", "ACTIVE", "count", 5),
            Map.of("status", "COMPLETED", "count", 12),
            Map.of("status", "PENDING", "count", 3)
        );
        
        return Map.of(
            "cohortStatus", cohortStatus
        );
    }
    
    public Map<String, Object> getCourseDistribution() {
        List<Map<String, Object>> courseDistribution = Arrays.asList(
            Map.of("course", "Digital Literacy", "participants", 85, "completion", 78.5),
            Map.of("course", "Leadership Development", "participants", 65, "completion", 82.3),
            Map.of("course", "Entrepreneurship", "participants", 45, "completion", 75.8)
        );
        
        return Map.of(
            "courseDistribution", courseDistribution
        );
    }
    
    // Helper methods for calculations
    private Double calculateCompletionRate() {
        return 76.5;
    }
    
    private Double calculateAverageScore() {
        return 78.3;
    }
    
    private Double calculateEmploymentRate() {
        return 65.2;
    }
    
    private Double calculateCourseCoverage() {
        return 85.7;
    }
    
    private Double calculateSurveyResponseRate() {
        // Calculate actual survey response rate from database
        long totalSurveys = surveyRepo.count();
        if (totalSurveys == 0) return 0.0;
        
        // This would be implemented with actual survey response calculations
        return 82.4; // Placeholder - replace with actual calculation
    }
    
    private Double calculateSurveyCompletionRate(Long surveyId) {
        // Calculate completion rate for specific survey from database
        // This would query survey responses and calculate percentage
        return 0.0; // Will be calculated from actual survey responses
    }
    
    // Legacy methods for backward compatibility
    private List<MEOfficerDashboardDTO.ParticipantOverview> getRecentParticipants() {
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
    
    public Map<String, Object> getAttendanceData(String cohort, String dateFrom, String dateTo) {
        return Map.of(
            "attendanceRate", 87.5,
            "totalSessions", 45,
            "averageAttendance", 22.3
        );
    }
    
    public Map<String, Object> getAssessmentData(String cohort, String assessmentType) {
        return Map.of(
            "averageScore", 78.3,
            "passRate", 92.0,
            "improvementRate", 15.2
        );
    }
    
    public Map<String, Object> getSurveyData(String surveyType) {
        return Map.of(
            "responseRate", 82.0,
            "satisfactionScore", 4.2,
            "completedSurveys", 124
        );
    }
    
    public Map<String, Object> getOutcomeData(String cohort) {
        return Map.of(
            "employmentRate", 76.5,
            "internshipRate", 45.2,
            "businessStarted", 12
        );
    }
    
    public Map<String, Object> exportReport(String reportType, String format) {
        return Map.of(
            "reportUrl", "/reports/" + reportType + "." + format,
            "generatedAt", new Date(),
            "status", "READY"
        );
    }
}