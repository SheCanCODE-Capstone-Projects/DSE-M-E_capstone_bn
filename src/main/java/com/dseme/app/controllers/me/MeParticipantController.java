package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.ParticipantResponseDTO;
import com.dseme.app.dtos.participants.CreateParticipantDTO;
import com.dseme.app.services.me.MeParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/participants")
@RequiredArgsConstructor
@Tag(name = "ME Participant Management", description = "View and manage all participants across all tracks")
public class MeParticipantController {

    private final MeParticipantService participantService;

    @GetMapping
    @Operation(summary = "Get all participants (paginated)")
    public ResponseEntity<Page<ParticipantResponseDTO>> getAllParticipants(
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(participantService.getAllParticipants(cohortId, batchId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get participant by ID")
    public ResponseEntity<ParticipantResponseDTO> getParticipantById(@PathVariable UUID id) {
        return ResponseEntity.ok(participantService.getParticipantById(id));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get participant statistics")
    public ResponseEntity<Object> getParticipantStats() {
        return ResponseEntity.ok(participantService.getParticipantStats());
    }

    @PostMapping
    @Operation(summary = "Create new participant")
    public ResponseEntity<ParticipantResponseDTO> createParticipant(@Valid @RequestBody CreateParticipantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(participantService.createParticipant(dto));
    }
}
