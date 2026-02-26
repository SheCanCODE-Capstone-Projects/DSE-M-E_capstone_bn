package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.AssignFacilitatorsDTO;
import com.dseme.app.dtos.me.FacilitatorSummaryDTO;
import com.dseme.app.services.me.MeCohortFacilitatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/cohorts/{cohortId}/facilitators")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ME_OFFICER')")
public class MeCohortFacilitatorController {

    private final MeCohortFacilitatorService facilitatorService;

    @GetMapping
    public ResponseEntity<List<FacilitatorSummaryDTO>> getFacilitators(@PathVariable UUID cohortId) {
        return ResponseEntity.ok(facilitatorService.getFacilitatorsByCohort(cohortId));
    }

    @PostMapping
    public ResponseEntity<List<FacilitatorSummaryDTO>> assignFacilitators(
            @PathVariable UUID cohortId,
            @Valid @RequestBody AssignFacilitatorsDTO dto
    ) {
        return ResponseEntity.ok(facilitatorService.assignFacilitators(cohortId, dto));
    }

    @DeleteMapping("/{facilitatorId}")
    public ResponseEntity<Void> removeFacilitator(
            @PathVariable UUID cohortId,
            @PathVariable UUID facilitatorId
    ) {
        facilitatorService.removeFacilitator(cohortId, facilitatorId);
        return ResponseEntity.noContent().build();
    }
}
