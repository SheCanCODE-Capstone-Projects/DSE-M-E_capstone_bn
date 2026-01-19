package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed DTO for facilitator profile.
 * Contains biographical data, activity logs, and performance trends.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorDetailDTO {
    /**
     * Facilitator ID (User ID).
     */
    private UUID facilitatorId;
    
    /**
     * Full name.
     */
    private String fullName;
    
    /**
     * Email address.
     */
    private String email;
    
    /**
     * Profile picture URL.
     */
    private String profilePictureUrl;
    
    /**
     * Specialization (e.g., "Digital Literacy", "Entrepreneurship").
     */
    private String specialization;
    
    /**
     * Years of experience.
     */
    private Integer yearsOfExperience;
    
    /**
     * Contact information.
     */
    private ContactInfoDTO contactInfo;
    
    /**
     * Availability status.
     */
    private AvailabilityStatus availabilityStatus;
    
    /**
     * Activity logs (latest survey sends, grade updates, etc.).
     */
    private List<ActivityLogDTO> activityLogs;
    
    /**
     * Performance trends (monthly student engagement over time).
     */
    private List<MonthlyEngagementDTO> performanceTrends;
    
    /**
     * Current workload metrics.
     */
    private WorkloadMetricsDTO workloadMetrics;
    
    /**
     * Performance indicators.
     */
    private PerformanceIndicatorsDTO performanceIndicators;
    
    /**
     * Timestamps.
     */
    private Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Nested DTO for contact information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfoDTO {
        private String phone;
        private String email;
        private String address;
    }
    
    /**
     * Nested DTO for workload metrics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkloadMetricsDTO {
        private Integer activeCohortsCount;
        private Integer totalParticipants;
        private Integer maxParticipantLoad;
        private BigDecimal participantLoadPercentage;
    }
    
    /**
     * Nested DTO for performance indicators.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceIndicatorsDTO {
        private BigDecimal averageParticipantScore;
        private BigDecimal facilitatorRating;
        private Boolean requiresSupport;
        private String supportReason;
    }
}
