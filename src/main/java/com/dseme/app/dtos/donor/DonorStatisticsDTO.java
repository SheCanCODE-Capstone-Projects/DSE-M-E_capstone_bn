package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonorStatisticsDTO {
    private Integer totalPartners;
    private Integer totalImpacted;
    private Double avgEmploymentRate;
    private Double budgetEfficiency;
}
