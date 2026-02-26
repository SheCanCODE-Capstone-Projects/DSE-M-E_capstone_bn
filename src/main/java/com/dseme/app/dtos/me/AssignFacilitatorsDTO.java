package com.dseme.app.dtos.me;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignFacilitatorsDTO {
    @NotEmpty(message = "At least one facilitator must be assigned")
    private List<UUID> facilitatorIds;
}
