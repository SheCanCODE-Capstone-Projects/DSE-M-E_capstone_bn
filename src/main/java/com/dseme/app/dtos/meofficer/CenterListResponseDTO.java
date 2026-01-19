package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated center list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterListResponseDTO {
    private List<CenterResponseDTO> centers;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
