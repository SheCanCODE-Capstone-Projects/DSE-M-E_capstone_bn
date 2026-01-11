package com.dseme.app.dtos.dashboard;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Data
@Builder
public class MEOfficerDashboardDTO {
    private DashboardStats stats;
    private List<ParticipantOverview> participants;
    private List<FacilitatorOverview> facilitators;
    private List<CohortOverview> cohorts;
    private List<AlertSummary> alerts;
    
    @Data
    @Builder
    public static class DashboardStats {
        private Long totalParticipants;
        private Long activeFacilitators;
        private Long activeCohorts;
        private Double completionRate;
        private Double avgScore;
        private Long pendingApprovals;
        private Double employmentRate;
    }
    
    @Data
    @Builder
    public static class ParticipantOverview {
        private UUID id;
        private String firstName;
        private String lastName;
        private String cohortCode;
        private String status;
        private Double averageScore;
        private String employmentStatus;
        private Instant enrollmentDate;
        private Boolean needsVerification;
    }
    
    @Data
    @Builder
    public static class FacilitatorOverview {
        private UUID id;
        private String name;
        private String email;
        private Long participantCount;
        private List<String> assignedCohorts;
        private Boolean isActive;
    }
    
    @Data
    @Builder
    public static class CohortOverview {
        private UUID id;
        private String cohortCode;
        private String cohortName;
        private String status;
        private Long participantCount;
        private Double completionRate;
        private Instant startDate;
        private Instant endDate;
    }
    
    @Data
    @Builder
    public static class AlertSummary {
        private String type;
        private String message;
        private String severity;
        private Long count;
        private Instant createdAt;
    }
}