package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for grouped dropout reasons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropoutReasonGroupDTO {

    /**
     * Dropout reason text (or "Not specified" if null).
     */
    private String reason;

    /**
     * Count of dropouts with this reason.
     */
    private Long count;

    /**
     * Percentage of total dropouts.
     */
    private BigDecimal percentage;
}
