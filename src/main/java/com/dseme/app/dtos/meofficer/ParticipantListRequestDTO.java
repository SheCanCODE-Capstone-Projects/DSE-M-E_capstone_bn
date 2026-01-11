package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ME_OFFICER participant list request with pagination and search.
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
     * Filter by verification status. Options: true (verified), false (unverified), null (all)
     */
    private Boolean verified;
}
