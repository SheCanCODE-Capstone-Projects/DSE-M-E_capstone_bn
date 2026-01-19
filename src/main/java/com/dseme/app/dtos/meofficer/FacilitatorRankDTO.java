package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Facilitator performance ranking data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorRankDTO {
    /**
     * Facilitator full name.
     */
    private String name;
    
    /**
     * Number of participants assigned to this facilitator.
     */
    private Integer participantCount;
    
    /**
     * Facilitator rating (out of 5.0).
     */
    private BigDecimal rating;
}
