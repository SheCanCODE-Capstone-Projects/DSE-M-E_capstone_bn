package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.*;
import com.dseme.app.services.me.MeFacilitatorService;
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
@RequestMapping("/api/me/facilitators")
@RequiredArgsConstructor
@Tag(name = "ME Facilitator Management", description = "Endpoints for managing facilitators in ME Portal")
public class MeFacilitatorController {

    private final MeFacilitatorService facilitatorService;

    @GetMapping
    @Operation(summary = "List all facilitators")
    public ResponseEntity<Page<FacilitatorResponseDTO>> getAllFacilitators(Pageable pageable) {
        Page<FacilitatorResponseDTO> facilitators = facilitatorService.getAllFacilitators(pageable);
        return ResponseEntity.ok(facilitators);
    }

    @PostMapping
    @Operation(summary = "Create new facilitator")
    public ResponseEntity<FacilitatorResponseDTO> createFacilitator(@Valid @RequestBody CreateFacilitatorDTO dto) {
        FacilitatorResponseDTO facilitator = facilitatorService.createFacilitator(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(facilitator);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update facilitator")
    public ResponseEntity<FacilitatorResponseDTO> updateFacilitator(
            @PathVariable UUID id, 
            @Valid @RequestBody CreateFacilitatorDTO dto) {
        FacilitatorResponseDTO facilitator = facilitatorService.updateFacilitator(id, dto);
        return ResponseEntity.ok(facilitator);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete facilitator")
    public ResponseEntity<Void> deleteFacilitator(@PathVariable UUID id) {
        facilitatorService.deleteFacilitator(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/courses")
    @Operation(summary = "Get facilitator's assigned courses")
    public ResponseEntity<List<AssignedCourseDTO>> getFacilitatorCourses(@PathVariable UUID id) {
        List<AssignedCourseDTO> courses = facilitatorService.getFacilitatorCourses(id);
        return ResponseEntity.ok(courses);
    }

    @PostMapping("/{id}/assign-course")
    @Operation(summary = "Assign course to facilitator")
    public ResponseEntity<Void> assignCourse(
            @PathVariable UUID id, 
            @Valid @RequestBody AssignCourseDTO dto) {
        facilitatorService.assignCourse(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    @Operation(summary = "Remove course assignment")
    public ResponseEntity<Void> removeCourseAssignment(
            @PathVariable UUID id, 
            @PathVariable UUID courseId) {
        facilitatorService.removeCourseAssignment(id, courseId);
        return ResponseEntity.noContent().build();
    }
}