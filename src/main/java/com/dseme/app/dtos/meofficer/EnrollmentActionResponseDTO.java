package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for enrollment approval/rejection response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentActionResponseDTO {
    private UUID enrollmentId;
    private Boolean isVerified;
    private EnrollmentStatus status;
    private String action; // "APPROVED" or "REJECTED"
    private String verifiedByName;
    private String verifiedByEmail;
    private Instant verifiedAt;
    private String message;
}
