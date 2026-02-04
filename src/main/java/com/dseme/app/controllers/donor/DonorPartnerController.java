package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.CreatePartnerRequestDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.PartnerResponseDTO;
import com.dseme.app.dtos.donor.UpdatePartnerRequestDTO;
import com.dseme.app.services.donor.DonorPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for DONOR partner organization management.
 * 
 * Provides portfolio-level partner management:
 * - Create partner organizations
 * - View all partners with metrics
 * 
 * All endpoints require DONOR role and are audit logged.
 */
@Tag(name = "Donor Partner Management", description = "Partner organization management endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/partners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorPartnerController extends DonorBaseController {

    private final DonorPartnerService partnerService;

    /**
     * Creates a new partner organization.
     * 
     * POST /api/donor/partners
     * 
     * Rules:
     * - Only DONOR can create partners
     * - Partner name must be unique
     * - Partner created as ACTIVE by default
     * - Audit log entry created
     */
    @Operation(
            summary = "Create partner organization",
            description = "Creates a new partner organization. " +
                    "Partner name must be unique. Partner is created as ACTIVE by default. " +
                    "Operation is audit logged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Partner created successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or duplicate partner name"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @PostMapping
    public ResponseEntity<PartnerResponseDTO> createPartner(
            HttpServletRequest request,
            @Valid @RequestBody CreatePartnerRequestDTO createRequest
    ) {
        DonorContext context = getDonorContext(request);
        
        PartnerResponseDTO createdPartner = partnerService.createPartner(context, createRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPartner);
    }

    /**
     * Gets all partner organizations with metrics.
     * 
     * GET /api/donor/partners
     * 
     * Includes:
     * - Partner status (isActive)
     * - Total programs
     * - Total cohorts
     */
    @Operation(
            summary = "Get all partner organizations",
            description = "Retrieves all partner organizations with metrics including " +
                    "partner status, total programs, and total cohorts."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partners retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping
    public ResponseEntity<List<PartnerResponseDTO>> getAllPartners(HttpServletRequest request) {
        DonorContext context = getDonorContext(request);
        
        List<PartnerResponseDTO> partners = partnerService.getAllPartners(context);
        
        return ResponseEntity.ok(partners);
    }

    /**
     * Gets a single partner organization by ID with metrics.
     * 
     * GET /api/donor/partners/{partnerId}
     */
    @Operation(
            summary = "Get partner by ID",
            description = "Retrieves a single partner organization by ID with metrics including " +
                    "partner status, total programs, and total cohorts."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partner retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Partner not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @GetMapping("/{partnerId}")
    public ResponseEntity<PartnerResponseDTO> getPartnerById(
            HttpServletRequest request,
            @PathVariable String partnerId
    ) {
        DonorContext context = getDonorContext(request);
        
        PartnerResponseDTO partner = partnerService.getPartnerById(context, partnerId);
        
        return ResponseEntity.ok(partner);
    }

    /**
     * Updates a partner organization.
     * 
     * PUT /api/donor/partners/{partnerId}
     */
    @Operation(
            summary = "Update partner organization",
            description = "Updates a partner organization. Partner name must be unique if changed. " +
                    "Operation is audit logged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partner updated successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid input or duplicate partner name"),
            @ApiResponse(responseCode = "404", description = "Partner not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @PutMapping("/{partnerId}")
    public ResponseEntity<PartnerResponseDTO> updatePartner(
            HttpServletRequest request,
            @PathVariable String partnerId,
            @Valid @RequestBody UpdatePartnerRequestDTO updateRequest
    ) {
        DonorContext context = getDonorContext(request);
        
        PartnerResponseDTO updatedPartner = partnerService.updatePartner(context, partnerId, updateRequest);
        
        return ResponseEntity.ok(updatedPartner);
    }

    /**
     * Activates or deactivates a partner organization.
     * 
     * PATCH /api/donor/partners/{partnerId}/status
     */
    @Operation(
            summary = "Update partner status",
            description = "Activates or deactivates a partner organization. " +
                    "Operation is audit logged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Partner status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Partner not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role")
    })
    @PatchMapping("/{partnerId}/status")
    public ResponseEntity<PartnerResponseDTO> updatePartnerStatus(
            HttpServletRequest request,
            @PathVariable String partnerId,
            @RequestParam Boolean isActive
    ) {
        DonorContext context = getDonorContext(request);
        
        PartnerResponseDTO updatedPartner = partnerService.updatePartnerStatus(context, partnerId, isActive);
        
        return ResponseEntity.ok(updatedPartner);
    }
}
