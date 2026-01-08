package com.dseme.app.filters;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.services.meofficer.MEOfficerAuthorizationService;
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
 * Filter that loads ME_OFFICER context for /api/me-officer/** endpoints.
 * 
 * This filter:
 * 1. Intercepts requests to /api/me-officer/**
 * 2. Extracts email from JWT token (already validated by JwtAuthenticationFilter)
 * 3. Loads ME_OFFICER context (partnerId, userId) from database
 * 4. Validates user has ME_OFFICER role, is active, and is verified
 * 5. Stores context in request attribute for controllers to use
 * 6. Returns 403 FORBIDDEN if ME_OFFICER context cannot be loaded
 * 
 * NOTE: Role validation is also handled by Spring Security in SecurityConfig.
 * This filter provides additional validation and context loading.
 * 
 * This runs AFTER JwtAuthenticationFilter but BEFORE controllers.
 * 
 * Performance: Context loading must complete in < 300ms.
 */
@Component
@RequiredArgsConstructor
public class MEOfficerAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    public static final String ME_OFFICER_CONTEXT_ATTRIBUTE = "meOfficerContext";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only process /api/me-officer/** endpoints
        if (!requestPath.startsWith("/api/me-officer/")) {
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

            // Load ME_OFFICER context (partnerId, userId)
            // Role validation is also done by Spring Security, but we validate here too
            MEOfficerContext context = meOfficerAuthorizationService.loadMEOfficerContext(email);

            // Store context in request attribute for controllers to use
            request.setAttribute(ME_OFFICER_CONTEXT_ATTRIBUTE, context);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (com.dseme.app.exceptions.AccessDeniedException e) {
            sendForbiddenResponse(response, e.getMessage());
        } catch (Exception e) {
            logger.error("Error in ME_OFFICER authorization filter", e);
            sendForbiddenResponse(response, "Access denied. Failed to load ME_OFFICER context.");
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

