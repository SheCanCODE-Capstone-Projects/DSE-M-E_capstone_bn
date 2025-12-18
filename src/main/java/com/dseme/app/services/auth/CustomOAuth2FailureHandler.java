package com.dseme.app.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom OAuth2 failure handler that returns JSON error responses
 * instead of redirecting to a login page.
 * 
 * This ensures REST API behavior - OAuth2 authentication failures
 * return proper JSON error responses instead of HTML redirects.
 */
@Component
public class CustomOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        // Set response content type to JSON
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        
        // Write JSON error response
        response.getWriter().write(
            "{\"error\": \"OAuth2 Authentication Failed\", \"message\": \"" + 
            exception.getMessage() + "\"}"
        );
        response.getWriter().flush();
    }
}
