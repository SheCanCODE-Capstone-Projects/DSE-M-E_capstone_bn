package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary DTO for facilitator list view.
 * Contains essential information for the paginated table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorSummaryDTO {
    /**
     * Facilitator ID (User ID).
     */
    private UUID id;
    
    /**
     * Full name (firstName + lastName).
     */
    private String fullName;
    
    /**
     * Profile picture URL (if available).
     */
    private String profilePictureUrl;
    
    /**
     * Number of active cohorts currently assigned.
     */
    private Integer activeCohortsCount;
    
    /**
     * Total number of participants under supervision.
     */
    private Integer totalParticipants;
    
    /**
     * Average participant score across all assessments.
     */
    private BigDecimal averageParticipantScore;
    
    /**
     * Facilitator rating from participant feedback surveys (e.g., 4.8/5.0).
     */
    private BigDecimal facilitatorRating;
    
    /**
     * Availability status.
     */
    private AvailabilityStatus availabilityStatus;
    
    /**
     * Flag indicating if facilitator requires support (performance below threshold).
     */
    @Builder.Default
    private Boolean requiresSupport = false;
}
