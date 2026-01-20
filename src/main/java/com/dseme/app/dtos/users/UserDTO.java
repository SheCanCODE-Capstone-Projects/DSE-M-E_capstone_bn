package com.dseme.app.dtos.users;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
    private Instant createdAt;
}
