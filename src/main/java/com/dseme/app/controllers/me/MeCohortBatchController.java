package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.CohortBatchResponseDTO;
import com.dseme.app.dtos.me.CreateCohortBatchDTO;
import com.dseme.app.dtos.me.UpdateCohortStatusDTO;
import com.dseme.app.services.me.MeCohortBatchService;
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
@RequestMapping("/api/me/cohort-batches")
@RequiredArgsConstructor
@Tag(name = "ME Cohort Batches", description = "Manage cohort batches/intakes (one cohort can have multiple course tracks)")
public class MeCohortBatchController {

    private final MeCohortBatchService batchService;

    @GetMapping
    @Operation(summary = "List cohort batches (paginated)")
    public ResponseEntity<Page<CohortBatchResponseDTO>> getAll(Pageable pageable) {
        return ResponseEntity.ok(batchService.getAllBatches(pageable));
    }

    @GetMapping("/list")
    @Operation(summary = "List cohort batches (full list for dropdowns)")
    public ResponseEntity<List<CohortBatchResponseDTO>> list() {
        return ResponseEntity.ok(batchService.getAllBatchesList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get cohort batch by ID")
    public ResponseEntity<CohortBatchResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    @PostMapping
    @Operation(summary = "Create cohort batch/intake")
    public ResponseEntity<CohortBatchResponseDTO> create(@Valid @RequestBody CreateCohortBatchDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update cohort batch status")
    public ResponseEntity<CohortBatchResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCohortStatusDTO dto) {
        return ResponseEntity.ok(batchService.updateBatchStatus(id, dto.getStatus()));
    }
}

