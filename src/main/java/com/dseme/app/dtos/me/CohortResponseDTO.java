package com.dseme.app.dtos.me;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortResponseDTO {
    private UUID id;
    private String name;
    private CourseSummaryDTO course;
    private FacilitatorSummaryDTO facilitator;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String status;
}