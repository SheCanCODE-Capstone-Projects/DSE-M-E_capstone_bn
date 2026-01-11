package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for participant verification response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantVerificationResponseDTO {
    private UUID participantId;
    private Boolean isVerified;
    private String verifiedByName;
    private String verifiedByEmail;
    private Instant verifiedAt;
    private String message;
}
