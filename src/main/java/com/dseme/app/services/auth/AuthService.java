package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.SignUpDTO;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.models.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public AuthService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public String signUp(SignUpDTO signUpDTO) {

        //Check if an email is not already taken
        if(userRepo.existsByEmail((signUpDTO.getEmail()))) {
            throw new ResourceAlreadyExistsException("User with email '" + signUpDTO.getEmail() + "' already exists!");
        }

        User user = new User();

        user.setEmail(signUpDTO.getEmail());
        user.setPasswordHash(encoder.encode(signUpDTO.getPassword()));
        user.setRole(Role.UNASSIGNED);

        userRepo.save(user);

        return user.getEmail() + " is successfully registered!";
    }

}