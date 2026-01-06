package com.dseme.app.filters;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.services.facilitator.FacilitatorAuthorizationService;
import com.dseme.app.utilities.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that loads facilitator context for /api/facilitator/** endpoints.
 * 
 * This filter:
 * 1. Intercepts requests to /api/facilitator/**
 * 2. Extracts email from JWT token (already validated by JwtAuthenticationFilter)
 * 3. Loads facilitator context (partnerId, centerId, cohortId) from database
 * 4. Stores context in request attribute for controllers to use
 * 5. Returns 403 FORBIDDEN if facilitator context cannot be loaded
 * 
 * NOTE: Role validation is handled by Spring Security in SecurityConfig.
 * This filter only loads the facilitator context after role validation passes.
 * 
 * This runs AFTER JwtAuthenticationFilter but BEFORE controllers.
 * 
 * Performance: Context loading must complete in < 300ms.
 */
@Component
@RequiredArgsConstructor
public class FacilitatorAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final FacilitatorAuthorizationService facilitatorAuthorizationService;

    public static final String FACILITATOR_CONTEXT_ATTRIBUTE = "facilitatorContext";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only process /api/facilitator/** endpoints
        if (!requestPath.startsWith("/api/facilitator/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from Authorization header
            String jwt = parseJwt(request);
            
            if (jwt == null || !jwtUtil.validateToken(jwt)) {
                sendForbiddenResponse(response, "Invalid or missing JWT token");
                return;
            }

            // Extract email from token (sub claim)
            String email = jwtUtil.getEmailFromToken(jwt);

            // Load facilitator context (partnerId, centerId, cohortId)
            // Role validation is already done by Spring Security
            FacilitatorContext context = facilitatorAuthorizationService.loadFacilitatorContext(email);

            // Store context in request attribute for controllers to use
            request.setAttribute(FACILITATOR_CONTEXT_ATTRIBUTE, context);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (com.dseme.app.exceptions.AccessDeniedException e) {
            sendForbiddenResponse(response, e.getMessage());
        } catch (Exception e) {
            logger.error("Error in facilitator authorization filter", e);
            sendForbiddenResponse(response, "Access denied. Failed to load facilitator context.");
        }
    }

    /**
     * Extracts JWT token from Authorization header.
     * Expected format: "Authorization: Bearer <token>"
     * 
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    /**
     * Sends a 403 FORBIDDEN response with JSON error message.
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            String.format("{\"error\": \"Forbidden\", \"message\": \"%s\"}", message)
        );
        response.getWriter().flush();
    }
}

