package com.dseme.app.dtos.me;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String studentId;
    private CohortSummaryDTO cohort;
    private LocalDate enrollmentDate;
    private String status;
    private BigDecimal score;
}