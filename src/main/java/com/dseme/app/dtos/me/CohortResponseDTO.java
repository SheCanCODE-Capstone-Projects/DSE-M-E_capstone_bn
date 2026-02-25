package com.dseme.app.dtos.me;

import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortResponseDTO {
    private UUID id;
    private String name;
    private UUID batchId;
    private String batchName;
    private CohortBatchSummaryDTO batch;
    private CourseSummaryDTO course;
    private List<FacilitatorSummaryDTO> facilitators;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String status;
}