package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    
    // Active Participants (for active cohort)
    private Long activeParticipantsCount;
    
    // Cohort Information
    private UUID cohortId;
    private String cohortName;
    private LocalDate cohortStartDate;
    private String programName;
    
    // Weekly Attendance Statistics
    private WeeklyAttendanceStats weeklyAttendance;
    
    // Overall Attendance Statistics
    private BigDecimal attendancePercentage;
    private Long totalAttendanceRecords;
    private Long expectedAttendanceRecords;
    private List<MissingAttendanceAlert> missingAttendanceAlerts;
    
    // Score Statistics
    private Long pendingScoresCount;
    private List<PendingScore> pendingScores;
    private BigDecimal averageScore;
    
    // Training Module Completion Rate (can be implemented later)
    private BigDecimal moduleCompletionRate;
    
    // Notifications
    private Long unreadNotificationsCount;
    private List<NotificationSummary> recentNotifications;
    
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

    /**
     * Weekly attendance statistics with comparison to previous week.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyAttendanceStats {
        /**
         * This week's attendance rate as percentage (0-100).
         */
        private BigDecimal thisWeekAttendanceRate;

        /**
         * Last week's attendance rate as percentage (0-100).
         */
        private BigDecimal lastWeekAttendanceRate;

        /**
         * Change in attendance rate from last week to this week.
         * Positive value means improvement, negative means decline.
         * Example: +3.5 means 3.5% increase from last week.
         */
        private BigDecimal changeFromLastWeek;

        /**
         * Formatted change string for display (e.g., "+3% from last week" or "-2% from last week").
         */
        private String changeDisplayText;

        /**
         * This week's start date (Monday of current week).
         */
        private LocalDate thisWeekStartDate;

        /**
         * This week's end date (Sunday of current week).
         */
        private LocalDate thisWeekEndDate;

        /**
         * Last week's start date (Monday of previous week).
         */
        private LocalDate lastWeekStartDate;

        /**
         * Last week's end date (Sunday of previous week).
         */
        private LocalDate lastWeekEndDate;

        /**
         * Number of present attendance records this week.
         */
        private Long thisWeekPresentCount;

        /**
         * Total expected attendance records this week.
         */
        private Long thisWeekExpectedCount;

        /**
         * Number of present attendance records last week.
         */
        private Long lastWeekPresentCount;

        /**
         * Total expected attendance records last week.
         */
        private Long lastWeekExpectedCount;
    }
}

