package com.dseme.app.dtos.me;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortBatchResponseDTO {
    private UUID id;
    private String name;
    private UUID centerId;
    private String centerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}

