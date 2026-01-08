package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.filters.MEOfficerAuthorizationFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Base controller for all ME_OFFICER endpoints.
 * 
 * Provides helper method to extract ME_OFFICER context from request attributes.
 * 
 * All ME_OFFICER controllers should extend this class and use getMEOfficerContext()
 * to access the partnerId and user information.
 */
public abstract class MEOfficerBaseController {

    /**
     * Extracts ME_OFFICER context from request attributes.
     * 
     * The context is set by MEOfficerAuthorizationFilter before the request reaches the controller.
     * 
     * @param request HTTP request
     * @return MEOfficerContext containing partnerId and user information
     * @throws IllegalStateException if context is not found (should never happen if filter is configured correctly)
     */
    protected MEOfficerContext getMEOfficerContext(HttpServletRequest request) {
        MEOfficerContext context = (MEOfficerContext) request.getAttribute(
            MEOfficerAuthorizationFilter.ME_OFFICER_CONTEXT_ATTRIBUTE
        );

        if (context == null) {
            throw new IllegalStateException(
                "ME_OFFICER context not found in request. " +
                "Ensure MEOfficerAuthorizationFilter is configured in SecurityConfig."
            );
        }

        return context;
    }
}

