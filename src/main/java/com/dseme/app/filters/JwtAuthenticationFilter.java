package com.dseme.app.filters;

import com.dseme.app.utilities.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter that processes JWT tokens from Authorization header.
 * 
 * This filter:
 * 1. Extracts JWT token from "Authorization: Bearer <token>" header
 * 2. Validates the token
 * 3. Extracts user email from token
 * 4. Loads user details and sets authentication in SecurityContext
 * 5. Always continues the filter chain (even if token is invalid/missing)
 * 
 * CRITICAL: This filter runs BEFORE UsernamePasswordAuthenticationFilter
 * (configured in SecurityConfig), ensuring JWT tokens are processed first.
 * 
 * When a valid JWT token is present, this filter sets the authentication in
 * SecurityContext, which prevents Spring Security from redirecting to the login page.
 * This fixes the 302 redirect issue when using JWT tokens after Google OAuth login.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            // Only process if JWT token exists and is valid
            if (jwt != null && jwtUtils.validateToken(jwt)) {
                String email = jwtUtils.getEmailFromToken(jwt);

                // Debug logging (can be removed in production)
                System.out.println("JWT Filter - email: " + email);

                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                System.out.println("JWT Filter - userDetails: " + userDetails);

                // Create authentication token with user details and authorities
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                System.out.println("JWT Filter - Authentication: " + authentication);

                // Set authentication details (IP address, session ID, etc.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext
                // This makes the user "authenticated" for the current request
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Log error but continue filter chain
            // If authentication fails, SecurityContext remains unauthenticated
            // and AuthEntryPointJwt will return JSON 401 response
            logger.error("Cannot set user authentication: ", e);
        }

        // CRITICAL: Always continue the filter chain
        // Even if token is missing or invalid, we continue to let other filters
        // and the authentication entry point handle the unauthenticated request
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header.
     * Expected format: "Authorization: Bearer <token>"
     * 
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    public String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
