package com.dseme.app.dtos.facilitator;

import com.dseme.app.models.Cohort;
import com.dseme.app.models.Partner;
import com.dseme.app.models.Center;
import com.dseme.app.models.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Context object that holds facilitator's authorization information.
 * This is loaded after JWT validation and contains all necessary IDs
 * for data access control.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorContext {
    private User facilitator;
    private String partnerId;
    private UUID centerId;
    private UUID cohortId;
    private Partner partner;
    private Center center;
    private Cohort cohort;
}

