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

import java.io.IOException;

/**
 * Custom OAuth2 success handler that generates a JWT token and redirects to
 * /api/auth/google endpoint where the frontend can retrieve the token.
 */
@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final OAuth2TokenStorage tokenStorage;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        User user = userRepo.findByEmail(oAuth2User.getEmail())
                .orElse(null);

        String token;

        if (user == null) {
            // Register a new user and generate token
            token = jwtUtil.generateToken(registerOAuth2User(oAuth2User));
        } else {
            // Return token for existing user
            token = jwtUtil.generateToken(oAuth2User.getEmail());
        }

        // Store the token temporarily and get a code
        String code = tokenStorage.storeToken(token);

        // Redirect to /api/auth/google with the code
        response.sendRedirect("/api/auth/google?code=" + code);
    }

    /**
     * Registers a new OAuth2 user in the database.
     * @param oAuth2User The OAuth2 user from Google
     * @return The email of the registered user
     */
    private String registerOAuth2User(CustomOAuth2User oAuth2User) {
        User user = new User();
        user.setEmail(oAuth2User.getEmail());
        user.setRole(Role.UNASSIGNED);
        user.setProvider(Provider.GOOGLE);
        user.setFirstName(oAuth2User.getName());
        user.setIsActive(true);
        // Note: passwordHash is null for OAuth users, which is handled in UserDetailService

        userRepo.save(user);

        return user.getEmail();
    }

}