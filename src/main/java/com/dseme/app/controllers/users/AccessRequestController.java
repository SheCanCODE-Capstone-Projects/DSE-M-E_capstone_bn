package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.AccessRequestResponseDTO;
import com.dseme.app.dtos.users.RoleRequestDTO;
import com.dseme.app.services.users.AccessRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Access Request Management", description = "Endpoints for managing role requests")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    @PostMapping("/users/request/role")
    @Operation(summary = "Request a role (UNASSIGNED users only)")
    public ResponseEntity<AccessRequestResponseDTO> requestRole(@Valid @RequestBody RoleRequestDTO dto) {
        AccessRequestResponseDTO response = accessRequestService.createRoleRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/access-requests")
    @Operation(summary = "List all access requests (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AccessRequestResponseDTO>> getAllRequests(Pageable pageable) {
        Page<AccessRequestResponseDTO> requests = accessRequestService.getAllRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/access-requests/pending")
    @Operation(summary = "List pending access requests (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AccessRequestResponseDTO>> getPendingRequests(Pageable pageable) {
        Page<AccessRequestResponseDTO> requests = accessRequestService.getPendingRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/access-requests/{id}/approve")
    @Operation(summary = "Approve access request (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccessRequestResponseDTO> approveRequest(@PathVariable UUID id) {
        AccessRequestResponseDTO response = accessRequestService.approveRequest(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/access-requests/{id}/reject")
    @Operation(summary = "Reject access request (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccessRequestResponseDTO> rejectRequest(@PathVariable UUID id) {
        AccessRequestResponseDTO response = accessRequestService.rejectRequest(id);
        return ResponseEntity.ok(response);
    }
}