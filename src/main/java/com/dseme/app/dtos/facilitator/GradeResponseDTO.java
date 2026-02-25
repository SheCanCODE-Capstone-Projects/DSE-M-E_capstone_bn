package com.dseme.app.dtos.facilitator;

import com.dseme.app.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeResponseDTO {
    private UUID scoreId;
    private UUID participantId;
    private String participantName;
    private UUID moduleId;
    private String moduleName;
    private AssessmentType assessmentType;
    private String assessmentName;
    private BigDecimal scoreValue;
    private String recordedByName;
    private Instant recordedAt;
}
