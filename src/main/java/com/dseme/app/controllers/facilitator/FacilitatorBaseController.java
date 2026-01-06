package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.filters.FacilitatorAuthorizationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base controller for all facilitator endpoints.
 * Provides helper method to extract FacilitatorContext from request.
 */
@RestController
public abstract class FacilitatorBaseController {

    /**
     * Extracts FacilitatorContext from request attribute.
     * This context is set by FacilitatorAuthorizationFilter after validation.
     * 
     * @param request HTTP request
     * @return FacilitatorContext containing facilitator's authorization info
     */
    protected FacilitatorContext getFacilitatorContext(HttpServletRequest request) {
        FacilitatorContext context = (FacilitatorContext) request.getAttribute(
                FacilitatorAuthorizationFilter.FACILITATOR_CONTEXT_ATTRIBUTE
        );
        
        if (context == null) {
            throw new IllegalStateException("FacilitatorContext not found in request. This should not happen if filter is configured correctly.");
        }
        
        return context;
    }
}

