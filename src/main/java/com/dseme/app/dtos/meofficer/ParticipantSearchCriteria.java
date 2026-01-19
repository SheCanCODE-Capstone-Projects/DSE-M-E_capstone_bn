package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Search criteria for participant filtering.
 * Supports full-text search, dropdown filters, and date range filtering.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSearchCriteria {
    /**
     * Full-text search query (searches name, email, participantCode).
     */
    private String search;
    
    /**
     * Filter by cohort ID.
     */
    private UUID cohortId;
    
    /**
     * Filter by facilitator ID.
     */
    private UUID facilitatorId;
    
    /**
     * Filter by enrollment status.
     */
    private EnrollmentStatus status;
    
    /**
     * Filter by enrollment date - start date.
     */
    private LocalDate enrollmentDateStart;
    
    /**
     * Filter by enrollment date - end date.
     */
    private LocalDate enrollmentDateEnd;
    
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
     * Sort field (default: "createdAt").
     */
    @Builder.Default
    private String sortBy = "createdAt";
    
    /**
     * Sort direction (ASC or DESC).
     */
    @Builder.Default
    private String sortDirection = "DESC";
}
