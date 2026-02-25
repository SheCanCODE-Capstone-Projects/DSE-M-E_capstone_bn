package com.dseme.app.dtos.facilitator;

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
public class ParticipantGradeDTO {
    private UUID participantId;
    private String participantName;
    private String participantEmail;
    private BigDecimal score;
    private Integer maxScore;
    private BigDecimal percentage;
    private String recordedByName;
    private Instant recordedAt;
}
