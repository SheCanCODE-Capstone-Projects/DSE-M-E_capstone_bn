package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ME_OFFICER enrollment list request with pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentListRequestDTO {
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
}
