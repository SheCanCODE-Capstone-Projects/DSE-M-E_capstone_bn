package com.dseme.app.services.auth;

import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.models.CustomOAuth2User;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.utilities.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Custom OAuth2 success handler that handles Google OAuth2 authentication.
 * 
 * For new users: Registers them, sets isVerified=false, isActive=false, sends verification email,
 * and redirects with a message to verify email.
 * 
 * For existing users: Checks if email is verified before allowing login.
 * If verified and active, generates JWT token. If not verified, redirects with message.
 */
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final OAuth2TokenStorage tokenStorage;
    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        User user = userRepo.findByEmail(oAuth2User.getEmail())
                .orElse(null);

        if (user == null) {
            // Register a new OAuth2 user
            user = registerOAuth2User(oAuth2User);
            // Send verification email
            emailVerificationService.generateAndSendVerificationToken(user);
            // Redirect with message to verify email (no JWT token yet)
            response.sendRedirect("/api/auth/google?message=Registration successful. Please check your email to verify your account.");
            return;
        }

        // Existing user - check if email is verified
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            // User not verified - redirect with message
            response.sendRedirect("/api/auth/google?error=Please verify your email first. Check your inbox for the verification link.");
            return;
        }

        // User is verified - check if account is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            response.sendRedirect("/api/auth/google?error=Your account is not active. Please contact support.");
            return;
        }

        // User is verified and active - generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());
        String code = tokenStorage.storeToken(token);
        response.sendRedirect("/api/auth/google?code=" + code);
    }

    /**
     * Registers a new OAuth2 user in the database.
     * Sets isVerified=false and isActive=false (same as regular registration).
     * 
     * @param oAuth2User The OAuth2 user from Google
     * @return The registered User entity
     */
    private User registerOAuth2User(CustomOAuth2User oAuth2User) {
        User user = new User();
        user.setEmail(oAuth2User.getEmail());
        user.setRole(Role.UNASSIGNED);
        user.setProvider(Provider.GOOGLE);
        user.setFirstName(oAuth2User.getName());
        user.setIsActive(false); // Same as regular registration - inactive until verified
        user.setIsVerified(false); // Must verify email before login
        // Note: passwordHash is null for OAuth users, which is handled in UserDetailService

        return userRepo.save(user);
    }

}