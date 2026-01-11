package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated ME_OFFICER participant list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantListResponseDTO {
    private List<ParticipantListDTO> participants;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
