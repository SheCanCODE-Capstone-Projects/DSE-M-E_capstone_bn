package com.dseme.app.dtos.me;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortBatchSummaryDTO {
    private UUID id;
    private String name;
}
