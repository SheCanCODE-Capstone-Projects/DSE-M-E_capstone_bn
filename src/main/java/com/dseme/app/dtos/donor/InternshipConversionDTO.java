package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for internship-to-employment conversion metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternshipConversionDTO {

    /**
     * Total completed internships.
     */
    private Long totalCompletedInternships;

    /**
     * Total participants who had internships and got employed.
     */
    private Long internshipsConvertedToEmployment;

    /**
     * Conversion rate (percentage).
     */
    private BigDecimal conversionRate;
}
