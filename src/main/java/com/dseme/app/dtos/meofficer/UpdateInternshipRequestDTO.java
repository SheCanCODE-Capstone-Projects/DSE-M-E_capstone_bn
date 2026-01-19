package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.InternshipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for updating an internship.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInternshipRequestDTO {
    private String organization;
    private String roleTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private InternshipStatus status;
    private BigDecimal stipendAmount;
}
