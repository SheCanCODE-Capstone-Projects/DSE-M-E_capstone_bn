package com.dseme.app.dtos.me;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetCohortBatchesDTO {
    @NotNull(message = "Cohort batch IDs list is required")
    private List<UUID> cohortBatchIds;
}
