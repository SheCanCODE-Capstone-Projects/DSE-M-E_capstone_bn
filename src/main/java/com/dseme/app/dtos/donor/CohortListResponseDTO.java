package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated cohort list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortListResponseDTO {

    private List<CohortSummaryDTO> cohorts;
    private Long totalCount;
    private int page;
    private int size;
    private int totalPages;
}
