package com.dseme.app.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequestDTO {

    @NotNull(message = "Partner id is mandatory")
    @NotBlank(message = "Partner id is mandatory")
    private String partnerId;           // "DSE201"

    @NotNull(message = "Requested Role is mandatory")
    @NotBlank(message = "Requested Role is mandatory")
    private String requestedRole;       // "PARTNER" | "ME_OFFICER" | "FACILITATOR"
}
