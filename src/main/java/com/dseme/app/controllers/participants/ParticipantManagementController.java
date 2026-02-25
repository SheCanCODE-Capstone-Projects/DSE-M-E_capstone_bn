package com.dseme.app.controllers.participants;

import com.dseme.app.dtos.me.ParticipantResponseDTO;
import com.dseme.app.dtos.participants.CreateParticipantDTO;
import com.dseme.app.dtos.participants.UpdateEmploymentDTO;
import com.dseme.app.dtos.participants.UpdateParticipantDTO;
import com.dseme.app.services.participants.ParticipantManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
@Tag(name = "Participant Management", description = "Endpoints for managing participants")
public class ParticipantManagementController {

    private final ParticipantManagementService participantService;

    @PostMapping
    @PreAuthorize("hasRole('FACILITATOR')")
    @Operation(summary = "Add participant (Facilitator only)")
    public ResponseEntity<ParticipantResponseDTO> addParticipant(@Valid @RequestBody CreateParticipantDTO dto) {
        ParticipantResponseDTO response = participantService.addParticipant(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FACILITATOR')")
    @Operation(summary = "Update participant info (Facilitator only)")
    public ResponseEntity<ParticipantResponseDTO> updateParticipant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParticipantDTO dto) {
        ParticipantResponseDTO response = participantService.updateParticipant(id, dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/employment")
    @PreAuthorize("hasRole('ME_OFFICER')")
    @Operation(summary = "Update employment status (ME Officer only)")
    public ResponseEntity<ParticipantResponseDTO> updateEmploymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmploymentDTO dto) {
        ParticipantResponseDTO response = participantService.updateEmploymentStatus(id, dto);
        return ResponseEntity.ok(response);
    }
}
