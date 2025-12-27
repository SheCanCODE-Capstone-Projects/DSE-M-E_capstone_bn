package com.dseme.app.services.users;

import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * User details service that loads user information for Spring Security.
 * 
 * IMPORTANT: Handles OAuth2 users (Google) who have null passwordHash.
 * For OAuth users, we use a placeholder password since they authenticate
 * via OAuth2, not password. The JWT filter validates the token, not the password.
 * 
 * Also checks isVerified and isActive status - user is enabled only if both are true.
 */
@Service
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepo;

    public UserDetailService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        // Handle OAuth2 users (Google) who have null passwordHash
        // For OAuth users, password is not used - authentication happens via JWT token
        // We use a placeholder password that will never be checked for OAuth users
        String password = user.getPasswordHash() != null 
            ? user.getPasswordHash() 
            : "{noop}oauth2_user"; // Placeholder password for OAuth users

        // User is enabled only if both isVerified and isActive are true
        boolean enabled = Boolean.TRUE.equals(user.getIsVerified()) 
                       && Boolean.TRUE.equals(user.getIsActive());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                enabled, // account enabled (verified and active)
                true,   // account non-expired
                true,   // credentials non-expired
                true,   // account non-locked
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
