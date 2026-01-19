package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for participant list.
 * Contains Page<ParticipantSummaryDTO> with pagination metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantListPageResponseDTO {
    /**
     * List of participant summaries.
     */
    private List<ParticipantSummaryDTO> content;
    
    /**
     * Total number of elements.
     */
    private Long totalElements;
    
    /**
     * Total number of pages.
     */
    private Integer totalPages;
    
    /**
     * Current page number (0-indexed).
     */
    private Integer currentPage;
    
    /**
     * Page size.
     */
    private Integer pageSize;
    
    /**
     * Whether there is a next page.
     */
    private Boolean hasNext;
    
    /**
     * Whether there is a previous page.
     */
    private Boolean hasPrevious;
}
