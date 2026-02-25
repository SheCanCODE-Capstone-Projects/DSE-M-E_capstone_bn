package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorProfileDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String organizationName;
    private String centerName;
}
