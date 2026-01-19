package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Search criteria for facilitator filtering.
 * Supports filtering by expertise, performance tier, location, and availability.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorSearchCriteria {
    /**
     * Full-text search query (searches name, email).
     */
    private String search;
    
    /**
     * Filter by subject area/expertise (e.g., "Entrepreneurship", "Digital Literacy").
     * Note: This may require adding a specialization field to User model or using program associations.
     */
    private String subjectArea;
    
    /**
     * Filter by performance tier - minimum rating threshold.
     * Used to find facilitators below threshold for "Review" actions.
     */
    private BigDecimal minRating;
    
    /**
     * Filter by maximum rating threshold.
     */
    private BigDecimal maxRating;
    
    /**
     * Filter by availability status.
     */
    private AvailabilityStatus availabilityStatus;
    
    /**
     * Filter by center ID (location/region).
     */
    private UUID centerId;
    
    /**
     * Filter by partner ID (for partner-level isolation).
     */
    private String partnerId;
    
    /**
     * Filter for facilitators requiring support (performance below threshold).
     */
    private Boolean requiresSupport;
    
    /**
     * Pagination - page number (0-indexed).
     */
    @Builder.Default
    private Integer page = 0;
    
    /**
     * Pagination - page size.
     */
    @Builder.Default
    private Integer size = 10;
    
    /**
     * Sort field (default: "fullName").
     */
    @Builder.Default
    private String sortBy = "fullName";
    
    /**
     * Sort direction (ASC or DESC).
     */
    @Builder.Default
    private String sortDirection = "ASC";
}
