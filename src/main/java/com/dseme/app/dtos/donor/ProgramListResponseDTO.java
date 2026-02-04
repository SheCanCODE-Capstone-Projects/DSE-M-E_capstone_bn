package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated program list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramListResponseDTO {

    private List<ProgramSummaryDTO> programs;
    private Long totalCount;
    private int page;
    private int size;
    private int totalPages;
}
