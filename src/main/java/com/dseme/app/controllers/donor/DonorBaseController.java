package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.filters.DonorAuthorizationFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Base controller for all DONOR endpoints.
 * 
 * Provides helper method to extract DONOR context from request attributes.
 * 
 * All DONOR controllers should extend this class and use getDonorContext()
 * to access the user information.
 * 
 * Unlike ME_OFFICER, DONOR does not have partnerId restriction
 * as they have portfolio-wide access across all partners.
 */
public abstract class DonorBaseController {

    /**
     * Extracts DONOR context from request attributes.
     * 
     * The context is set by DonorAuthorizationFilter before the request reaches the controller.
     * 
     * @param request HTTP request
     * @return DonorContext containing user information
     * @throws IllegalStateException if context is not found (should never happen if filter is configured correctly)
     */
    protected DonorContext getDonorContext(HttpServletRequest request) {
        DonorContext context = (DonorContext) request.getAttribute(
            DonorAuthorizationFilter.DONOR_CONTEXT_ATTRIBUTE
        );

        if (context == null) {
            throw new IllegalStateException(
                "DONOR context not found in request. " +
                "Ensure DonorAuthorizationFilter is configured in SecurityConfig."
            );
        }

        return context;
    }
}
