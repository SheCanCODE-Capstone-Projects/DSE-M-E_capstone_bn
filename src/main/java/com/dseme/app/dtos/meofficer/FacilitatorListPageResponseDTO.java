package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated response for facilitator list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorListPageResponseDTO {
    /**
     * List of facilitator summaries.
     */
    private List<FacilitatorSummaryDTO> content;
    
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
