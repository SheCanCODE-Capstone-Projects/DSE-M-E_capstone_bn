package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorCohortDTO {
    private UUID cohortId;
    private String cohortName;
    private String courseName;
    private String courseCode;
    private String batchName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer totalParticipants;
    private Integer activeParticipants;
}
