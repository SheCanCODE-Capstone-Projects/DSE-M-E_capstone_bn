package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.enums.EmploymentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating an employment outcome.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmploymentOutcomeRequestDTO {
    @NotNull(message = "Enrollment ID is required")
    private UUID enrollmentId;

    /**
     * Optional internship ID if employment is related to an internship.
     */
    private UUID internshipId;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    private String employerName;

    private String jobTitle;

    private EmploymentType employmentType;

    private String salaryRange;

    private BigDecimal monthlyAmount;

    private LocalDate startDate;

    /**
     * Optional verification flag.
     * If true, sets verifiedBy to ME_OFFICER and verifiedAt to current timestamp.
     */
    private Boolean verified;
}
