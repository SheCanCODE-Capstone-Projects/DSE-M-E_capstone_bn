package com.dseme.app.dtos.me;

import com.dseme.app.enums.CohortStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCohortStatusDTO {
    @NotNull(message = "Status is required")
    private CohortStatus status;
}
