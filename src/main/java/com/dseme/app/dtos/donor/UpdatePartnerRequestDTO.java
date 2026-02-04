package com.dseme.app.dtos.donor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating partner organization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePartnerRequestDTO {

    /**
     * Partner name.
     */
    private String partnerName;

    /**
     * Country.
     */
    private String country;

    /**
     * Region.
     */
    private String region;

    /**
     * Contact person name.
     */
    private String contactPerson;

    /**
     * Contact email.
     */
    @Email(message = "Contact email must be a valid email address")
    private String contactEmail;

    /**
     * Contact phone.
     */
    private String contactPhone;
}
