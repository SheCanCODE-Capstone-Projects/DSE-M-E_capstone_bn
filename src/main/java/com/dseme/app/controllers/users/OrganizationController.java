package com.dseme.app.controllers.users;

import com.dseme.app.models.Center;
import com.dseme.app.models.Partner;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.PartnerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Endpoints for fetching organizations and locations")
public class OrganizationController {

    private final PartnerRepository partnerRepository;
    private final CenterRepository centerRepository;

    @PostMapping("/partners")
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Create a new partner (organization) - DONOR only")
    public ResponseEntity<PartnerDTO> createPartner(@Valid @RequestBody CreatePartnerRequest request) {
        Partner partner = new Partner();
        // Partner IDs are String primary keys (no @GeneratedValue),
        // so we must assign an ID before persisting.
        partner.setPartnerId("DSE" + java.util.UUID.randomUUID());
        partner.setPartnerName(request.getName());
        // Default country to Rwanda if not provided
        partner.setCountry(request.getCountry() != null && !request.getCountry().isBlank()
                ? request.getCountry()
                : "Rwanda");
        // Use provided region (e.g., province) or empty string
        partner.setRegion(request.getRegion() != null ? request.getRegion() : "");
        // For now, use name as contact person if none provided
        partner.setContactPerson(
                request.getContactPerson() != null && !request.getContactPerson().isBlank()
                        ? request.getContactPerson()
                        : request.getName()
        );
        partner.setContactEmail(request.getEmail());
        partner.setContactPhone(request.getPhone());
        partner.setIsActive(true);

        Partner saved = partnerRepository.save(partner);

        PartnerDTO body = new PartnerDTO(
                saved.getPartnerId(),
                saved.getPartnerName(),
                saved.getCountry(),
                saved.getRegion(),
                saved.getContactEmail(),
                saved.getContactPhone()
        );

        return ResponseEntity.ok(body);
    }

    @PostMapping("/partners/{partnerId}/centers")
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Create a new center (branch/location) for a partner - DONOR only")
    public ResponseEntity<CenterDTO> createCenter(
            @PathVariable String partnerId,
            @Valid @RequestBody CreateCenterRequest request
    ) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found with id: " + partnerId));

        Center center = new Center();
        center.setPartner(partner);
        center.setCenterName(request.getCenterName());
        center.setLocation(request.getLocation());
        // Default country/region from request or fall back to partner's values
        center.setCountry(
                request.getCountry() != null && !request.getCountry().isBlank()
                        ? request.getCountry()
                        : partner.getCountry()
        );
        center.setRegion(
                request.getRegion() != null && !request.getRegion().isBlank()
                        ? request.getRegion()
                        : partner.getRegion()
        );
        center.setIsActive(true);

        Center saved = centerRepository.save(center);

        CenterDTO body = new CenterDTO(
                saved.getId(),
                saved.getPartner().getPartnerId(),
                saved.getCenterName(),
                saved.getLocation(),
                saved.getCountry(),
                saved.getRegion()
        );

        return ResponseEntity.ok(body);
    }

    @GetMapping("/partners")
    @Operation(summary = "Get all active partners (organizations)")
    public ResponseEntity<List<PartnerDTO>> getAllActivePartners() {
        List<PartnerDTO> partners = partnerRepository.findAll().stream()
                .filter(partner -> Boolean.TRUE.equals(partner.getIsActive()))
                .map(partner -> new PartnerDTO(
                        partner.getPartnerId(),
                        partner.getPartnerName(),
                        partner.getCountry(),
                        partner.getRegion(),
                        partner.getContactEmail(),
                        partner.getContactPhone()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/centers")
    @Operation(summary = "Get all active centers (locations)")
    public ResponseEntity<List<CenterDTO>> getAllActiveCenters() {
        List<CenterDTO> centers = centerRepository.findAll().stream()
                .filter(center -> Boolean.TRUE.equals(center.getIsActive()))
                .map(center -> new CenterDTO(
                        center.getId(),
                        center.getPartner().getPartnerId(),
                        center.getCenterName(),
                        center.getLocation(),
                        center.getCountry(),
                        center.getRegion()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(centers);
    }

    @GetMapping("/partners/{partnerId}/centers")
    @Operation(summary = "Get all active centers for a specific partner")
    public ResponseEntity<List<CenterDTO>> getCentersByPartner(@PathVariable String partnerId) {
        List<CenterDTO> centers = centerRepository.findAll().stream()
                .filter(center -> center.getPartner().getPartnerId().equals(partnerId))
                .filter(center -> Boolean.TRUE.equals(center.getIsActive()))
                .map(center -> new CenterDTO(
                        center.getId(),
                        center.getPartner().getPartnerId(),
                        center.getCenterName(),
                        center.getLocation(),
                        center.getCountry(),
                        center.getRegion()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(centers);
    }

    @Data
    public static class CreatePartnerRequest {
        @NotBlank
        private String name;

        @NotBlank
        private String email;

        private String phone;

        /**
         * Optional: display country. Defaults to "Rwanda" if not provided.
         */
        private String country;

        /**
         * Optional: region/province for the partner.
         */
        private String region;

        /**
         * Optional: contact person for the partner. Defaults to name if not provided.
         */
        private String contactPerson;
    }

    @Data
    public static class CreateCenterRequest {
        @NotBlank
        private String centerName;

        /**
         * Free-text location, e.g. "Kigali - Gasabo" or "Huye".
         */
        @NotBlank
        private String location;

        /**
         * Optional: center country. Defaults to partner country if not provided.
         */
        private String country;

        /**
         * Optional: region/province. Defaults to partner region if not provided.
         */
        private String region;
    }

    @Data
    @RequiredArgsConstructor
    public static class PartnerDTO {
        private final String partnerId;
        private final String partnerName;
        private final String country;
        private final String region;
        private final String contactEmail;
        private final String contactPhone;
    }

    @Data
    @RequiredArgsConstructor
    public static class CenterDTO {
        private final UUID centerId;
        private final String partnerId;
        private final String centerName;
        private final String location;
        private final String country;
        private final String region;
    }
}
