package com.dseme.app.controllers.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.services.meofficer.MEOfficerReportService;
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
import java.util.List;
import java.util.UUID;

/**
 * Controller for ME_OFFICER report export operations.
 * 
 * All endpoints enforce partner-level data isolation.
 * Reports are partner-scoped only.
 */
@Tag(name = "ME Officer Reports", description = "Report export endpoints for ME_OFFICER role")
@RestController
@RequestMapping("/api/me-officer/reports")
@RequiredArgsConstructor
public class MEOfficerReportController extends MEOfficerBaseController {

    private final MEOfficerReportService reportService;

    /**
     * Exports reports in CSV or PDF format.
     * Partner-scoped only. Large exports are async-ready.
     * 
     * GET /api/me-officer/reports/export
     * 
     * Query Parameters:
     * - reportType: PARTICIPANTS, ATTENDANCE, SCORES, OUTCOMES, SURVEYS, COMPREHENSIVE (default: COMPREHENSIVE)
     * - format: CSV or PDF (default: CSV, PDF not yet implemented)
     * - cohortId: Optional cohort ID to filter data
     * - startDate: Optional start date for date-range reports
     * - endDate: Optional end date for date-range reports
     * - surveyId: Optional survey ID for survey reports
     */
    @Operation(
            summary = "Export report",
            description = "Exports reports in CSV or PDF format. " +
                    "Reports are partner-scoped only. " +
                    "Large exports are async-ready (currently synchronous, can be enhanced for async processing)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report exported successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid report type or format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role or is not assigned to a partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Survey or cohort not found")
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "COMPREHENSIVE") String reportType,
            @RequestParam(required = false, defaultValue = "CSV") String format,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(required = false) UUID surveyId
    ) throws IOException {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ExportReportRequestDTO exportRequest = ExportReportRequestDTO.builder()
                .reportType(reportType)
                .format(format)
                .cohortId(cohortId)
                .startDate(startDate)
                .endDate(endDate)
                .surveyId(surveyId)
                .build();
        
        byte[] reportData = reportService.exportReport(context, exportRequest);
        
        String filename = String.format("%s_report_%s.%s", 
                reportType.toLowerCase(),
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                format.toLowerCase());
        
        MediaType contentType = "PDF".equalsIgnoreCase(format) ? 
                MediaType.APPLICATION_PDF : 
                MediaType.parseMediaType("text/csv");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(contentType)
                .body(reportData);
    }

    /**
     * Gets all available reports for the partner.
     * Returns metadata for generated documents.
     * 
     * GET /api/me-officer/reports/available
     */
    @Operation(
            summary = "Get available reports",
            description = "Retrieves metadata for all available report documents for the ME_OFFICER's partner. " +
                    "Includes report name, period, file size, format, and download URL. " +
                    "Reports are sorted by generation date (most recent first)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available reports retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ME_OFFICER role")
    })
    @GetMapping("/available")
    public ResponseEntity<List<ReportDocumentDTO>> getAvailableReports(HttpServletRequest request) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        List<ReportDocumentDTO> reports = reportService.getAvailableReports(context);
        
        return ResponseEntity.ok(reports);
    }

    /**
     * Gets report document metadata by ID.
     * 
     * GET /api/me-officer/reports/{documentId}
     */
    @Operation(
            summary = "Get report document metadata",
            description = "Retrieves metadata for a specific report document. " +
                    "Includes report name, period, file size, format, and download URL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report document metadata retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Report does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Report document not found")
    })
    @GetMapping("/{documentId}")
    public ResponseEntity<ReportDocumentDTO> getReportDocument(
            HttpServletRequest request,
            @Parameter(description = "Document ID (ReportSnapshot ID)") @PathVariable UUID documentId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ReportDocumentDTO document = reportService.getReportDocument(context, documentId);
        
        return ResponseEntity.ok(document);
    }

    /**
     * Downloads a report document.
     * 
     * GET /api/me-officer/reports/download/{documentId}
     */
    @Operation(
            summary = "Download report document",
            description = "Downloads a report document by ID. " +
                    "Returns the file as a blob stream (CSV, PDF, or JSON format)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report document downloaded successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Report does not belong to your partner"),
            @ApiResponse(responseCode = "404", description = "Not Found - Report document not found")
    })
    @GetMapping("/download/{documentId}")
    public ResponseEntity<byte[]> downloadReportDocument(
            HttpServletRequest request,
            @Parameter(description = "Document ID (ReportSnapshot ID)") @PathVariable UUID documentId
    ) {
        MEOfficerContext context = getMEOfficerContext(request);
        
        ReportDocumentDTO document = reportService.getReportDocument(context, documentId);
        byte[] reportData = reportService.getReportDocumentData(context, documentId);
        
        MediaType contentType = switch (document.getFileFormat().toUpperCase()) {
            case "PDF" -> MediaType.APPLICATION_PDF;
            case "JSON" -> MediaType.APPLICATION_JSON;
            default -> MediaType.parseMediaType("text/csv");
        };
        
        String filename = document.getReportName().replaceAll("[^a-zA-Z0-9.-]", "_") + 
                "." + document.getFileFormat().toLowerCase();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(contentType)
                .body(reportData);
    }
}
