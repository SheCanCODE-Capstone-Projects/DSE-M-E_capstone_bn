package com.dseme.app.services.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication entry point that returns JSON error responses
 * instead of redirecting to login page or returning HTML error pages.
 * 
 * This is critical for REST API behavior - when an unauthenticated request
 * hits a protected endpoint, this returns a proper JSON 401 response.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        // Set response content type to JSON
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        // Write JSON error response
        response.getWriter().write(
            "{\"error\": \"Unauthorized\", \"message\": \"Authentication required. Please provide a valid JWT token.\"}"
        );
    }
}
