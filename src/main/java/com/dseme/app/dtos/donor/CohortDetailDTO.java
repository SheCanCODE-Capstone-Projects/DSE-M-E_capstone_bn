package com.dseme.app.dtos.donor;

import com.dseme.app.enums.CohortStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for detailed cohort information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortDetailDTO {

    private UUID id;
    private String cohortName;
    private LocalDate startDate;
    private LocalDate endDate;
    private CohortStatus status;
    private Integer targetEnrollment;
    private Long actualEnrollment;
    private BigDecimal completionRate;
    private String programId;
    private String programName;
    private String centerId;
    private String centerName;
    private String partnerId;
    private String partnerName;
    private Instant createdAt;
}
