package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.InternshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for internship response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipResponseDTO {
    private UUID internshipId;
    private UUID enrollmentId;
    private String organization;
    private String roleTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private InternshipStatus status;
    private BigDecimal stipendAmount;
    private String createdByName;
    private String createdByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
