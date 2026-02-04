package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated alert list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertListResponseDTO {

    private List<AlertSummaryDTO> alerts;
    private Long totalCount;
    private Long unresolvedCount;
    private int page;
    private int size;
    private int totalPages;
}
