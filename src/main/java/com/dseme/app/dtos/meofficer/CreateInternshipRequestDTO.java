package com.dseme.app.dtos.meofficer;

import com.dseme.app.enums.InternshipStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating an internship placement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInternshipRequestDTO {
    @NotNull(message = "Enrollment ID is required")
    private UUID enrollmentId;

    @NotBlank(message = "Organization is required")
    private String organization;

    private String roleTitle;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Status is required")
    private InternshipStatus status;

    private BigDecimal stipendAmount;
}
