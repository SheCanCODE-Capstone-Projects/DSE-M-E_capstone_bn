package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for participant list request with pagination, search, filter, and sort.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantListRequestDTO {
    /**
     * Page number (0-indexed). Default: 0
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * Page size. Default: 10
     */
    @Builder.Default
    private Integer size = 10;

    /**
     * Search term to filter participants by name, email, phone.
     */
    private String search;

    /**
     * Sort field. Options: firstName, lastName, email, phone, enrollmentDate, attendancePercentage, enrollmentStatus
     */
    @Builder.Default
    private String sortBy = "firstName";

    /**
     * Sort direction. Options: ASC, DESC. Default: ASC
     */
    @Builder.Default
    private String sortDirection = "ASC";

    /**
     * Filter by enrollment status. Options: ACTIVE, INACTIVE, COMPLETED, DROPPED_OUT, WITHDRAWN
     */
    private String enrollmentStatusFilter;

    /**
     * Filter by gender. Options: MALE, FEMALE, OTHER
     */
    private String genderFilter;
}

