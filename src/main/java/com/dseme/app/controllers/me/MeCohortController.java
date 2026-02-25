package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.CohortResponseDTO;
import com.dseme.app.dtos.me.CreateCohortDTO;
import com.dseme.app.dtos.me.UpdateCohortStatusDTO;
import com.dseme.app.services.me.MeCohortService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/cohorts")
@RequiredArgsConstructor
@Tag(name = "ME Cohort Management", description = "Create and manage cohorts: each cohort = one course + one facilitator + participants (e.g. same org, different courses/facilitators)")
public class MeCohortController {

    private final MeCohortService cohortService;

    @GetMapping
    @Operation(summary = "List all cohorts (paginated)")
    public ResponseEntity<Page<CohortResponseDTO>> getAllCohorts(Pageable pageable) {
        return ResponseEntity.ok(cohortService.getAllCohorts(pageable));
    }

    @GetMapping("/list")
    @Operation(summary = "List all cohorts (full list for dropdowns)")
    public ResponseEntity<List<CohortResponseDTO>> getAllCohortsList() {
        return ResponseEntity.ok(cohortService.getAllCohortsList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cohort by ID")
    public ResponseEntity<CohortResponseDTO> getCohortById(@PathVariable UUID id) {
        return ResponseEntity.ok(cohortService.getCohortById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new cohort (course + optional facilitator)")
    public ResponseEntity<CohortResponseDTO> createCohort(@Valid @RequestBody CreateCohortDTO dto) {
        CohortResponseDTO created = cohortService.createCohort(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update cohort")
    public ResponseEntity<CohortResponseDTO> updateCohort(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCohortDTO dto) {
        return ResponseEntity.ok(cohortService.updateCohort(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update cohort status (PLANNED, ACTIVE, INACTIVE, COMPLETED)")
    public ResponseEntity<CohortResponseDTO> updateCohortStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCohortStatusDTO dto) {
        return ResponseEntity.ok(cohortService.updateCohortStatus(id, dto.getStatus()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cohort (only if no participants)")
    public ResponseEntity<Void> deleteCohort(@PathVariable UUID id) {
        cohortService.deleteCohort(id);
        return ResponseEntity.noContent().build();
    }
}
