package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for facilitator dashboard data.
 * Contains aggregated statistics and alerts for the facilitator's active cohort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorDashboardDTO {
    
    // Enrollment Statistics
    private Long enrollmentCount;
    private Long activeEnrollments;
    private Long completedEnrollments;
    private Long droppedOutEnrollments;
    
    // Attendance Statistics
    private BigDecimal attendancePercentage;
    private Long totalAttendanceRecords;
    private Long expectedAttendanceRecords;
    private List<MissingAttendanceAlert> missingAttendanceAlerts;
    
    // Score Statistics
    private Long pendingScoresCount;
    private List<PendingScore> pendingScores;
    private BigDecimal averageScore;
    
    // Notifications
    private Long unreadNotificationsCount;
    private List<NotificationSummary> recentNotifications;
    
    // Cohort Information
    private UUID cohortId;
    private String cohortName;
    private String programName;
    
    // Additional Statistics
    private Long totalParticipants;
    private Long totalModules;
    private Long completedModules;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingAttendanceAlert {
        private UUID participantId;
        private String participantName;
        private UUID enrollmentId;
        private String moduleName;
        private java.time.LocalDate sessionDate;
        private String reason; // e.g., "No attendance recorded for this date"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingScore {
        private UUID enrollmentId;
        private UUID participantId;
        private String participantName;
        private UUID moduleId;
        private String moduleName;
        private String assessmentType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationSummary {
        private UUID notificationId;
        private String title;
        private String message;
        private String notificationType;
        private String priority;
        private Boolean isRead;
        private Instant createdAt;
    }
}

