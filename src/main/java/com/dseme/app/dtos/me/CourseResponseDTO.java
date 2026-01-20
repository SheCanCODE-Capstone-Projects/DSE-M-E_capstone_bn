package com.dseme.app.dtos.me;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String level;
    private Integer durationWeeks;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String status;
    private List<FacilitatorSummaryDTO> facilitators;
}