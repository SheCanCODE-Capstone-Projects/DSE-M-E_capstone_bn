package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.services.facilitator.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Controller for exporting data to CSV format.
 * 
 * All endpoints require:
 * - Authentication (JWT token)
 * - Role: FACILITATOR
 * - Active cohort assignment
 */
@Tag(name = "Export", description = "APIs for exporting data to CSV format")
@RestController
@RequestMapping("/api/facilitator/export")
@RequiredArgsConstructor
public class ExportController extends FacilitatorBaseController {

    private final ExportService exportService;

    /**
     * Exports participant list to CSV.
     * 
     * GET /api/facilitator/export/participants
     */
    @Operation(
        summary = "Export participants",
        description = "Exports participant list with enrollment details and attendance percentage to CSV."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV file generated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/participants")
    public ResponseEntity<byte[]> exportParticipants(HttpServletRequest request) throws IOException {
        FacilitatorContext context = getFacilitatorContext(request);
        
        byte[] csvData = exportService.exportParticipants(context);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=participants.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    /**
     * Exports attendance report to CSV.
     * 
     * GET /api/facilitator/export/attendance?moduleId={moduleId}&startDate={startDate}&endDate={endDate}
     */
    @Operation(
        summary = "Export attendance",
        description = "Exports attendance report for a specific module within a date range to CSV."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV file generated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/attendance")
    public ResponseEntity<byte[]> exportAttendance(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @RequestParam UUID moduleId,
            @Parameter(description = "Start date (YYYY-MM-DD)") @RequestParam LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)") @RequestParam LocalDate endDate
    ) throws IOException {
        FacilitatorContext context = getFacilitatorContext(request);
        
        byte[] csvData = exportService.exportAttendance(context, moduleId, startDate, endDate);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    /**
     * Exports grade report to CSV.
     * 
     * GET /api/facilitator/export/grades?moduleId={moduleId}
     */
    @Operation(
        summary = "Export grades",
        description = "Exports grade report for a specific module to CSV."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV file generated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/grades")
    public ResponseEntity<byte[]> exportGrades(
            HttpServletRequest request,
            @Parameter(description = "Training module ID") @RequestParam UUID moduleId
    ) throws IOException {
        FacilitatorContext context = getFacilitatorContext(request);
        
        byte[] csvData = exportService.exportGrades(context, moduleId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grades.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    /**
     * Exports outcomes report to CSV.
     * 
     * GET /api/facilitator/export/outcomes
     */
    @Operation(
        summary = "Export outcomes",
        description = "Exports participant employment outcomes report to CSV."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV file generated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/outcomes")
    public ResponseEntity<byte[]> exportOutcomes(HttpServletRequest request) throws IOException {
        FacilitatorContext context = getFacilitatorContext(request);
        
        byte[] csvData = exportService.exportOutcomes(context);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=outcomes.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    /**
     * Exports survey responses to CSV.
     * 
     * GET /api/facilitator/export/surveys/{surveyId}
     */
    @Operation(
        summary = "Export survey responses",
        description = "Exports survey responses with participant answers to CSV."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV file generated successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/surveys/{surveyId}")
    public ResponseEntity<byte[]> exportSurveyResponses(
            HttpServletRequest request,
            @Parameter(description = "Survey ID") @PathVariable UUID surveyId
    ) throws IOException {
        FacilitatorContext context = getFacilitatorContext(request);
        
        byte[] csvData = exportService.exportSurveyResponses(context, surveyId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=survey_responses.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}

