package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.ExportReportRequestDTO;
import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER report generation and export.
 * 
 * Enforces strict partner-level data isolation.
 * All exports are partner-scoped only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerReportService {

    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScoreRepository scoreRepository;
    private final EmploymentOutcomeRepository employmentOutcomeRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;
    private final ReportSnapshotRepository reportSnapshotRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Formats file size in bytes to human-readable string.
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Exports report in CSV or PDF format.
     * Partner-scoped only. Large exports are async-ready.
     * 
     * @param context ME_OFFICER context
     * @param request Export request with report type and format
     * @return Report data as byte array
     * @throws IOException if export fails
     */
    public byte[] exportReport(
            MEOfficerContext context,
            ExportReportRequestDTO request
    ) throws IOException {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        String reportType = request.getReportType() != null ? 
                request.getReportType().toUpperCase() : "COMPREHENSIVE";
        String format = request.getFormat() != null ? 
                request.getFormat().toUpperCase() : "CSV";

        // Generate report data (CSV format internally)
        byte[] csvData = switch (reportType) {
            case "PARTICIPANTS" -> exportParticipantsReport(context, request);
            case "ATTENDANCE" -> exportAttendanceReport(context, request);
            case "SCORES" -> exportScoresReport(context, request);
            case "OUTCOMES" -> exportOutcomesReport(context, request);
            case "SURVEYS" -> exportSurveysReport(context, request);
            case "COMPREHENSIVE" -> exportComprehensiveReport(context, request);
            default -> throw new IllegalArgumentException("Invalid report type: " + reportType);
        };

        // Convert to PDF if requested
        if ("PDF".equals(format)) {
            return convertCsvToPdf(csvData, reportType, context);
        }

        return csvData;
    }

    /**
     * Exports participants report.
     */
    private byte[] exportParticipantsReport(MEOfficerContext context, ExportReportRequestDTO request) throws IOException {
        List<Participant> participants;
        if (request.getCohortId() != null) {
            // Get participants enrolled in the specified cohort
            List<Enrollment> enrollments = enrollmentRepository.findByCohortId(request.getCohortId());
            participants = enrollments.stream()
                    .map(Enrollment::getParticipant)
                    .filter(p -> p.getPartner().getPartnerId().equals(context.getPartnerId()))
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            // Get all participants for partner
            participants = participantRepository.findByPartnerPartnerId(
                    context.getPartnerId(),
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)
            ).getContent();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.println("Participant ID,First Name,Last Name,Email,Phone,Gender,Date of Birth,Education Level,Employment Status Baseline,Is Verified,Verified By,Verified At,Created At");

        for (Participant participant : participants) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    participant.getId(),
                    escapeCsv(participant.getFirstName()),
                    escapeCsv(participant.getLastName()),
                    escapeCsv(participant.getEmail()),
                    escapeCsv(participant.getPhone()),
                    participant.getGender() != null ? participant.getGender().name() : "",
                    participant.getDateOfBirth() != null ? participant.getDateOfBirth().format(DATE_FORMATTER) : "",
                    escapeCsv(participant.getEducationLevel()),
                    participant.getEmploymentStatusBaseline() != null ? participant.getEmploymentStatusBaseline().name() : "",
                    participant.getIsVerified() != null ? participant.getIsVerified() : false,
                    participant.getVerifiedBy() != null ? escapeCsv(participant.getVerifiedBy().getEmail()) : "",
                    participant.getVerifiedAt() != null ? participant.getVerifiedAt().toString() : "",
                    participant.getCreatedAt() != null ? participant.getCreatedAt().toString() : ""
            );
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports attendance report.
     */
    private byte[] exportAttendanceReport(MEOfficerContext context, ExportReportRequestDTO request) throws IOException {
        List<Attendance> attendances;
        if (request.getCohortId() != null) {
            attendances = attendanceRepository.findByParticipantPartnerPartnerIdAndCohortId(
                    context.getPartnerId(),
                    request.getCohortId()
            );
        } else {
            attendances = attendanceRepository.findByParticipantPartnerPartnerId(context.getPartnerId());
        }

        // Filter by date range if provided
        if (request.getStartDate() != null || request.getEndDate() != null) {
            LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.MIN;
            LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.MAX;
            attendances = attendances.stream()
                    .filter(a -> !a.getSessionDate().isBefore(startDate) && !a.getSessionDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("Participant Name,Session Date,Status,Module Name,Cohort Name,Remarks");

        for (Attendance attendance : attendances) {
            Participant participant = attendance.getEnrollment().getParticipant();
            writer.printf("%s,%s,%s,%s,%s,%s%n",
                    escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                    attendance.getSessionDate().format(DATE_FORMATTER),
                    attendance.getStatus().name(),
                    escapeCsv(attendance.getModule().getModuleName()),
                    escapeCsv(attendance.getEnrollment().getCohort().getCohortName()),
                    escapeCsv(attendance.getRemarks())
            );
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports scores report.
     */
    private byte[] exportScoresReport(MEOfficerContext context, ExportReportRequestDTO request) throws IOException {
        // Get all enrollments for partner
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantPartnerPartnerId(
                context.getPartnerId()
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("Participant Name,Cohort Name,Module Name,Assessment Type,Assessment Name,Score,Max Score,Percentage,Is Validated,Validated By,Assessment Date");

        for (Enrollment enrollment : enrollments) {
            List<Score> scores = scoreRepository.findByEnrollmentId(enrollment.getId());
            Participant participant = enrollment.getParticipant();

            for (Score score : scores) {
                BigDecimal percentage = score.getMaxScore() != null && score.getMaxScore().compareTo(BigDecimal.ZERO) > 0
                        ? score.getScoreValue().divide(score.getMaxScore(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                writer.printf("%s,%s,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s,%s%n",
                        escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                        escapeCsv(enrollment.getCohort().getCohortName()),
                        escapeCsv(score.getModule().getModuleName()),
                        score.getAssessmentType().name(),
                        escapeCsv(score.getAssessmentName()),
                        score.getScoreValue().doubleValue(),
                        score.getMaxScore() != null ? score.getMaxScore().doubleValue() : 100.0,
                        percentage.doubleValue(),
                        score.getIsValidated() != null ? score.getIsValidated() : false,
                        score.getValidatedBy() != null ? escapeCsv(score.getValidatedBy().getEmail()) : "",
                        score.getAssessmentDate() != null ? score.getAssessmentDate().format(DATE_FORMATTER) : ""
                );
            }
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports employment outcomes report.
     */
    private byte[] exportOutcomesReport(MEOfficerContext context, ExportReportRequestDTO request) throws IOException {
        // Get all enrollments for partner
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantPartnerPartnerId(
                context.getPartnerId()
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("Participant Name,Cohort Name,Employment Status,Employer Name,Job Title,Employment Type,Salary Range,Monthly Amount,Start Date,Verified,Verified By");

        for (Enrollment enrollment : enrollments) {
            List<EmploymentOutcome> outcomes = employmentOutcomeRepository.findByEnrollmentId(enrollment.getId());
            Participant participant = enrollment.getParticipant();

            for (EmploymentOutcome outcome : outcomes) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%s%n",
                        escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                        escapeCsv(enrollment.getCohort().getCohortName()),
                        outcome.getEmploymentStatus().name(),
                        escapeCsv(outcome.getEmployerName()),
                        escapeCsv(outcome.getJobTitle()),
                        outcome.getEmploymentType() != null ? outcome.getEmploymentType().name() : "",
                        escapeCsv(outcome.getSalaryRange()),
                        outcome.getMonthlyAmount() != null ? outcome.getMonthlyAmount().doubleValue() : 0.0,
                        outcome.getStartDate() != null ? outcome.getStartDate().format(DATE_FORMATTER) : "",
                        outcome.getVerified() != null ? outcome.getVerified() : false,
                        outcome.getVerifiedBy() != null ? escapeCsv(outcome.getVerifiedBy().getEmail()) : ""
                );
            }
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports surveys report.
     */
    private byte[] exportSurveysReport(MEOfficerContext context, ExportReportRequestDTO request) throws IOException {
        if (request.getSurveyId() == null) {
            throw new IllegalArgumentException("Survey ID is required for survey reports");
        }

        Survey survey = surveyRepository.findById(request.getSurveyId())
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found"));

        if (!survey.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException("Survey does not belong to your partner");
        }

        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(request.getSurveyId());
        List<SurveyQuestion> questions = surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(request.getSurveyId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.print("Participant ID,Submitted At");
        for (SurveyQuestion question : questions) {
            writer.print("," + escapeCsv(question.getQuestionText()));
        }
        writer.println();

        // Write data rows
        for (SurveyResponse response : responses) {
            writer.print(response.getParticipant().getId() + ",");
            writer.print(response.getSubmittedAt() != null ? response.getSubmittedAt().toString() : "Pending");

            for (SurveyQuestion question : questions) {
                String answer = response.getAnswers().stream()
                        .filter(a -> a.getQuestion().getId().equals(question.getId()))
                        .map(a -> a.getAnswerValue() != null ? a.getAnswerValue() : "")
                        .findFirst()
                        .orElse("");
                writer.print("," + escapeCsv(answer));
            }
            writer.println();
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports comprehensive report (all data).
     */
    private byte[] exportComprehensiveReport(MEOfficerContext context, ExportReportRequestDTO request) throws IOException {
        // Combine multiple reports into one comprehensive export
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        writer.println("=== COMPREHENSIVE PARTNER REPORT ===");
        writer.println("Partner: " + context.getPartner().getPartnerName());
        writer.println("Generated: " + java.time.LocalDateTime.now());
        writer.println();

        // Participants section
        writer.println("=== PARTICIPANTS ===");
        byte[] participantsData = exportParticipantsReport(context, request);
        writer.write(new String(participantsData));
        writer.println();

        // Attendance section
        writer.println("=== ATTENDANCE ===");
        byte[] attendanceData = exportAttendanceReport(context, request);
        writer.write(new String(attendanceData));
        writer.println();

        // Scores section
        writer.println("=== SCORES ===");
        byte[] scoresData = exportScoresReport(context, request);
        writer.write(new String(scoresData));
        writer.println();

        // Outcomes section
        writer.println("=== EMPLOYMENT OUTCOMES ===");
        byte[] outcomesData = exportOutcomesReport(context, request);
        writer.write(new String(outcomesData));

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Escapes CSV special characters.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Converts CSV data to PDF format using Apache PDFBox.
     */
    private byte[] convertCsvToPdf(byte[] csvData, String reportType, MEOfficerContext context) throws IOException {
        try {
            org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument();
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            document.addPage(page);

            org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = 
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);

            // Set up fonts
            org.apache.pdfbox.pdmodel.font.PDType1Font titleFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
            org.apache.pdfbox.pdmodel.font.PDType1Font headerFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
            org.apache.pdfbox.pdmodel.font.PDType1Font bodyFont = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;

            float margin = 50;
            float yPosition = page.getMediaBox().getHeight() - margin;
            float lineHeight = 14;
            float fontSize = 10;

            // Write title
            contentStream.beginText();
            contentStream.setFont(titleFont, 16);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(reportType + " Report - " + context.getPartner().getPartnerName());
            contentStream.endText();
            yPosition -= lineHeight * 2;

            // Write metadata
            contentStream.beginText();
            contentStream.setFont(bodyFont, fontSize);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Generated: " + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            contentStream.endText();
            yPosition -= lineHeight * 2;

            // Parse CSV and write to PDF
            String csvString = new String(csvData);
            String[] lines = csvString.split("\n");
            
            boolean isHeader = true;
            for (String line : lines) {
                if (yPosition < margin + lineHeight) {
                    // Create new page
                    contentStream.close();
                    page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - margin;
                }

                // Truncate line if too long for PDF
                String displayLine = line.length() > 100 ? line.substring(0, 97) + "..." : line;
                
                contentStream.beginText();
                contentStream.setFont(isHeader ? headerFont : bodyFont, fontSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(displayLine.replace(",", " | ")); // Replace commas with pipes for readability
                contentStream.endText();
                yPosition -= lineHeight;
                
                isHeader = false;
            }

            contentStream.close();

            // Convert to byte array
            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
            document.save(pdfOutputStream);
            document.close();

            return pdfOutputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error converting CSV to PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all available reports for the partner.
     * Returns metadata for generated documents.
     * 
     * @param context ME_OFFICER context
     * @return List of report documents
     */
    public List<com.dseme.app.dtos.meofficer.ReportDocumentDTO> getAvailableReports(MEOfficerContext context) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get all report snapshots for partner
        List<ReportSnapshot> snapshots = reportSnapshotRepository.findByPartnerPartnerId(context.getPartnerId());

        return snapshots.stream()
                .map(snapshot -> mapToReportDocumentDTO(snapshot, context))
                .sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt())) // Most recent first
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Gets report document by ID.
     * 
     * @param context ME_OFFICER context
     * @param documentId Document ID (ReportSnapshot ID)
     * @return Report document DTO
     */
    public com.dseme.app.dtos.meofficer.ReportDocumentDTO getReportDocument(
            MEOfficerContext context,
            UUID documentId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        ReportSnapshot snapshot = reportSnapshotRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report document not found with ID: " + documentId
                ));

        // Validate snapshot belongs to partner
        if (!snapshot.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Report document does not belong to your assigned partner."
            );
        }

        return mapToReportDocumentDTO(snapshot, context);
    }

    /**
     * Maps ReportSnapshot to ReportDocumentDTO.
     */
    private com.dseme.app.dtos.meofficer.ReportDocumentDTO mapToReportDocumentDTO(
            ReportSnapshot snapshot,
            MEOfficerContext context
    ) {
        // Determine report type from reportType string
        com.dseme.app.enums.ReportType reportType = determineReportType(snapshot.getReportType());
        
        // Format period string
        String period = formatPeriod(snapshot.getReportPeriodStart(), snapshot.getReportPeriodEnd());
        
        // Format file size
        String fileSize = snapshot.getFileSizeBytes() != null ?
                formatFileSize(snapshot.getFileSizeBytes()) : "Unknown";
        
        // Generate download URL
        String downloadUrl = "/api/me-officer/reports/download/" + snapshot.getId();

        return com.dseme.app.dtos.meofficer.ReportDocumentDTO.builder()
                .documentId(snapshot.getId())
                .reportName(generateReportName(snapshot.getReportType(), snapshot.getReportPeriodStart()))
                .reportType(reportType)
                .period(period)
                .periodStart(snapshot.getReportPeriodStart())
                .periodEnd(snapshot.getReportPeriodEnd())
                .fileSize(fileSize)
                .fileSizeBytes(snapshot.getFileSizeBytes())
                .fileFormat(snapshot.getFileFormat())
                .downloadUrl(downloadUrl)
                .generatedByName(snapshot.getGeneratedBy() != null ?
                        snapshot.getGeneratedBy().getFirstName() + " " + 
                        snapshot.getGeneratedBy().getLastName() : "System")
                .generatedAt(snapshot.getGeneratedAt())
                .build();
    }

    /**
     * Determines ReportType enum from report type string.
     */
    private com.dseme.app.enums.ReportType determineReportType(String reportTypeString) {
        if (reportTypeString == null) {
            return com.dseme.app.enums.ReportType.CUSTOM;
        }
        
        String upper = reportTypeString.toUpperCase();
        if (upper.contains("MONTHLY")) {
            return com.dseme.app.enums.ReportType.MONTHLY;
        } else if (upper.contains("QUARTERLY")) {
            return com.dseme.app.enums.ReportType.QUARTERLY;
        } else if (upper.contains("ANNUAL")) {
            return com.dseme.app.enums.ReportType.ANNUAL;
        } else {
            return com.dseme.app.enums.ReportType.CUSTOM;
        }
    }

    /**
     * Formats period string from start and end dates.
     */
    private String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null) {
            return "Unknown";
        }
        
        if (end == null || start.equals(end)) {
            return start.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
        }
        
        // Check if same month
        if (start.getYear() == end.getYear() && start.getMonth() == end.getMonth()) {
            return start.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
        }
        
        // Check if same year
        if (start.getYear() == end.getYear()) {
            return start.format(java.time.format.DateTimeFormatter.ofPattern("MMM")) + " - " +
                   end.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
        }
        
        return start.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")) + " - " +
               end.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
    }

    /**
     * Generates report name from report type and period.
     */
    private String generateReportName(String reportType, LocalDate periodStart) {
        String typeName = reportType != null ? reportType.replace("_", " ") : "Report";
        String periodStr = periodStart != null ?
                periodStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")) : "";
        
        return typeName + (periodStr.isEmpty() ? "" : " - " + periodStr);
    }

    /**
     * Gets report document data for download.
     * 
     * @param context ME_OFFICER context
     * @param documentId Document ID (ReportSnapshot ID)
     * @return Report data as byte array
     */
    public byte[] getReportDocumentData(MEOfficerContext context, UUID documentId) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        ReportSnapshot snapshot = reportSnapshotRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report document not found with ID: " + documentId
                ));

        // Validate snapshot belongs to partner
        if (!snapshot.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                    "Access denied. Report document does not belong to your assigned partner."
            );
        }

        // Return report data as bytes
        // Note: In a real implementation, you might store files in blob storage
        // For now, we return the stored reportData as bytes
        return snapshot.getReportData() != null ? 
                snapshot.getReportData().getBytes() : 
                new byte[0];
    }
}
