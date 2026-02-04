package com.dseme.app.dtos.donor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new partner organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePartnerRequestDTO {

    @NotBlank(message = "Partner name is required")
    @Size(max = 255, message = "Partner name must not exceed 255 characters")
    private String partnerName;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    private String country;

    @Size(max = 100, message = "Region must not exceed 100 characters")
    private String region;

    @Size(max = 255, message = "Contact person must not exceed 255 characters")
    private String contactPerson;

    @Email(message = "Contact email must be a valid email address")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 50, message = "Contact phone must not exceed 50 characters")
    private String contactPhone;
}
