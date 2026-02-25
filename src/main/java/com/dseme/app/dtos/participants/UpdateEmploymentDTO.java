package com.dseme.app.dtos.participants;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmploymentDTO {
    
    private String employmentStatus;
    private BigDecimal annualIncome;
}
