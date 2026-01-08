package com.dseme.app.dtos.meofficer;

import com.dseme.app.models.Partner;
import com.dseme.app.models.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Context object for ME_OFFICER requests.
 * Contains all necessary information for partner-level data isolation.
 * 
 * This context is loaded by MEOfficerAuthorizationFilter and stored in request attributes.
 * All ME_OFFICER services and controllers use this context to ensure partner-level isolation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MEOfficerContext {
    /**
     * The ME_OFFICER user entity.
     */
    private User meOfficer;
    
    /**
     * Partner ID (String) - used for filtering queries.
     */
    private String partnerId;
    
    /**
     * Partner entity - for reference.
     */
    private Partner partner;
    
    /**
     * User ID (UUID) - for audit trails.
     */
    private UUID userId;
}

