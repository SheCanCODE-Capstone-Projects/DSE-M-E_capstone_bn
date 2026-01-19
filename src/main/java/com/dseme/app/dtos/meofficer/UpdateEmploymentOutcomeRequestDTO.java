package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.enums.EmploymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for updating an employment outcome.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmploymentOutcomeRequestDTO {
    private UUID internshipId;
    private EmploymentStatus employmentStatus;
    private String employerName;
    private String jobTitle;
    private EmploymentType employmentType;
    private String salaryRange;
    private BigDecimal monthlyAmount;
    private LocalDate startDate;
    private Boolean verified;
}
