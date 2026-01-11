package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.enums.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for employment outcome response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmploymentOutcomeResponseDTO {
    private UUID employmentOutcomeId;
    private UUID enrollmentId;
    private UUID internshipId;
    private EmploymentStatus employmentStatus;
    private String employerName;
    private String jobTitle;
    private EmploymentType employmentType;
    private String salaryRange;
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private Boolean verified;
    private String verifiedByName;
    private String verifiedByEmail;
    private Instant verifiedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
