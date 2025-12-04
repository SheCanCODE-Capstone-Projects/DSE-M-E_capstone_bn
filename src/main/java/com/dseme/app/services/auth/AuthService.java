package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.LoginDTO;
import com.dseme.app.dtos.auth.SignUpDTO;
import com.dseme.app.exceptions.AccountInactiveException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.models.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.utilies.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public AuthService(UserRepository userRepo,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public String signUp(SignUpDTO signUpDTO) {
        if (userRepo.existsByEmail(signUpDTO.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User with email '" + signUpDTO.getEmail() + "' already exists!"
            );
        }

        User user = new User();
        user.setEmail(signUpDTO.getEmail());
        user.setPasswordHash(encoder.encode(signUpDTO.getPassword()));
        user.setRole(Role.UNASSIGNED);

        userRepo.save(user);

        return user.getEmail() + " is successfully registered!";
    }

    public String login(LoginDTO loginDTO) {
        User user = userRepo.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + loginDTO.getEmail()
                ));

        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new AccountInactiveException("Your account is not active");
        }

        if (!encoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getEmail(),
                        loginDTO.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        return jwtUtil.generateToken(userDetails.getUsername());
    }
}
