package com.dseme.app.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPassword(
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,
        
        @NotBlank(message = "Password confirmation is required")
        String repeatPassword
) {}
