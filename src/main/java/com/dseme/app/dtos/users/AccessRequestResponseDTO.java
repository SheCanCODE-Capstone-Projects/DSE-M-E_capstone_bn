package com.dseme.app.dtos.users;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestResponseDTO {
    private UUID id;
    private String requesterEmail;
    private String requesterName;
    private String requestedRole;
    private String reason;
    private String status;
    private Instant requestedAt;
    private Instant reviewedAt;
    private String reviewedBy;
}