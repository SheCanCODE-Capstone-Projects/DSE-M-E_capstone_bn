package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.ExportReportRequestDTO;
import com.dseme.app.services.donor.DonorReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for DONOR report export operations.
 * 
 * Provides portfolio-wide aggregated reports in CSV and PDF formats.
 * All reports are aggregated only - no participant-level data is exposed.
 */
@Tag(name = "Donor Reports", description = "Report export endpoints for DONOR role")
@RestController
@RequestMapping("/api/donor/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorReportController extends DonorBaseController {

    private final DonorReportService reportService;

    /**
     * Exports portfolio-wide report in CSV or PDF format.
     * 
     * GET /api/donor/reports/export
     * 
     * Query parameters:
     * - reportType: ENROLLMENT, EMPLOYMENT, DEMOGRAPHIC, COMPREHENSIVE (default: COMPREHENSIVE)
     * - format: CSV or PDF (default: CSV)
     * - dateRangeStart: Optional start date
     * - dateRangeEnd: Optional end date
     * - partnerId: Optional partner filter
     */
    @Operation(
            summary = "Export portfolio-wide report",
            description = "Exports portfolio-wide aggregated reports in CSV or PDF format. " +
                    "Supports multiple report types: ENROLLMENT, EMPLOYMENT, DEMOGRAPHIC, COMPREHENSIVE. " +
                    "All data is aggregated only - no participant-level data is exposed. " +
                    "Async-safe for large datasets."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report exported successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid report type or format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have DONOR role"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Report generation failed")
    })
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "COMPREHENSIVE") String reportType,
            @RequestParam(required = false, defaultValue = "CSV") String format,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateRangeStart,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateRangeEnd,
            @RequestParam(required = false) String partnerId
    ) {
        DonorContext context = getDonorContext(request);

        ExportReportRequestDTO exportRequest = ExportReportRequestDTO.builder()
                .reportType(reportType)
                .format(format)
                .dateRangeStart(dateRangeStart)
                .dateRangeEnd(dateRangeEnd)
                .partnerId(partnerId)
                .build();

        try {
            byte[] reportData = reportService.exportReport(context, exportRequest);

            // Determine content type
            MediaType contentType = "PDF".equalsIgnoreCase(format) ? 
                    MediaType.APPLICATION_PDF : 
                    MediaType.parseMediaType("text/csv");

            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("portfolio_%s_report_%s.%s", 
                    reportType.toLowerCase(), timestamp, format.toLowerCase());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(contentType)
                    .contentLength(reportData.length)
                    .body(reportData);

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report: " + e.getMessage(), e);
        }
    }
}
