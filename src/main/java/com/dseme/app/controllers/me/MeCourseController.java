package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.*;
import com.dseme.app.services.me.MeCourseService;
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
@RequestMapping("/api/me/courses")
@RequiredArgsConstructor
@Tag(name = "ME Course Administration", description = "Endpoints for managing courses in ME Portal")
public class MeCourseController {

    private final MeCourseService courseService;

    @GetMapping
    @Operation(summary = "List all courses")
    public ResponseEntity<Page<CourseResponseDTO>> getAllCourses(Pageable pageable) {
        Page<CourseResponseDTO> courses = courseService.getAllCourses(pageable);
        return ResponseEntity.ok(courses);
    }

    @PostMapping
    @Operation(summary = "Create new course")
    public ResponseEntity<CourseResponseDTO> createCourse(@Valid @RequestBody CreateCourseDTO dto) {
        CourseResponseDTO course = courseService.createCourse(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update course")
    public ResponseEntity<CourseResponseDTO> updateCourse(
            @PathVariable UUID id, 
            @Valid @RequestBody CreateCourseDTO dto) {
        CourseResponseDTO course = courseService.updateCourse(id, dto);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Get course participants")
    public ResponseEntity<List<ParticipantResponseDTO>> getCourseParticipants(@PathVariable UUID id) {
        List<ParticipantResponseDTO> participants = courseService.getCourseParticipants(id);
        return ResponseEntity.ok(participants);
    }
}