package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for exporting data to CSV format.
 * Handles exports for participants, attendance, grades, outcomes, and surveys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExportService {

    // Removed unused participantRepository
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScoreRepository scoreRepository;
    private final EmploymentOutcomeRepository employmentOutcomeRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final SurveyQuestionRepository surveyQuestionRepository;
    private final CohortIsolationService cohortIsolationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Exports participant list to CSV.
     */
    public byte[] exportParticipants(FacilitatorContext context) throws IOException {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.println("Participant ID,First Name,Last Name,Email,Phone,Gender,Enrollment Date,Enrollment Status,Attendance %");

        // Write data rows
        for (Enrollment enrollment : enrollments) {
            Participant participant = enrollment.getParticipant();
            BigDecimal attendancePercentage = calculateAttendancePercentage(enrollment);
            String displayStatus = enrollment.getStatus().name(); // Simplified - can use EnrollmentStatusService if needed

            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%.2f%n",
                    participant.getId(),
                    escapeCsv(participant.getFirstName()),
                    escapeCsv(participant.getLastName()),
                    escapeCsv(participant.getEmail()),
                    escapeCsv(participant.getPhone()),
                    participant.getGender() != null ? participant.getGender().name() : "",
                    enrollment.getEnrollmentDate() != null ? enrollment.getEnrollmentDate().format(DATE_FORMATTER) : "",
                    displayStatus,
                    attendancePercentage.doubleValue()
            );
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports attendance report to CSV.
     */
    public byte[] exportAttendance(FacilitatorContext context, UUID moduleId, java.time.LocalDate startDate, java.time.LocalDate endDate) throws IOException {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.println("Participant Name,Date,Status,Check-in Time,Module");

        // Get attendances for the date range
        List<Attendance> attendances = attendanceRepository.findByEnrollmentIdInAndModuleIdAndSessionDateBetween(
                enrollments.stream().map(Enrollment::getId).collect(Collectors.toList()),
                moduleId,
                startDate,
                endDate
        );

        for (Attendance attendance : attendances) {
            Participant participant = attendance.getEnrollment().getParticipant();
            writer.printf("%s,%s,%s,%s,%s%n",
                    escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                    attendance.getSessionDate().format(DATE_FORMATTER),
                    attendance.getStatus().name(),
                    attendance.getCreatedAt() != null ? attendance.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).format(DATETIME_FORMATTER) : "",
                    escapeCsv(attendance.getModule().getModuleName())
            );
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports grade report to CSV.
     */
    public byte[] exportGrades(FacilitatorContext context, UUID moduleId) throws IOException {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Enrollment> enrollments = enrollmentRepository.findByCohortId(context.getCohortId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.println("Participant Name,Assessment Type,Assessment Name,Score,Max Score,Percentage,Date Recorded");

        for (Enrollment enrollment : enrollments) {
            List<Score> scores = scoreRepository.findByEnrollmentIdAndModuleId(enrollment.getId(), moduleId);
            for (Score score : scores) {
                Participant participant = enrollment.getParticipant();
                BigDecimal percentage = score.getMaxScore() != null && score.getMaxScore().compareTo(BigDecimal.ZERO) > 0
                        ? score.getScoreValue().divide(score.getMaxScore(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                writer.printf("%s,%s,%s,%.2f,%.2f,%.2f,%s%n",
                        escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                        score.getAssessmentType().name(),
                        escapeCsv(score.getAssessmentName()),
                        score.getScoreValue().doubleValue(),
                        score.getMaxScore() != null ? score.getMaxScore().doubleValue() : 100.0,
                        percentage.doubleValue(),
                        score.getAssessmentDate() != null ? score.getAssessmentDate().format(DATE_FORMATTER) :
                                (score.getRecordedAt() != null ? score.getRecordedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FORMATTER) : "")
                );
            }
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports outcomes report to CSV.
     */
    public byte[] exportOutcomes(FacilitatorContext context) throws IOException {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        List<EmploymentOutcome> outcomes = employmentOutcomeRepository.findByCohortId(context.getCohortId());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.println("Participant Name,Status,Company Name,Position,Start Date,Monthly Amount,Employment Type");

        for (EmploymentOutcome outcome : outcomes) {
            Participant participant = outcome.getEnrollment().getParticipant();
            writer.printf("%s,%s,%s,%s,%s,%.2f,%s%n",
                    escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                    outcome.getEmploymentStatus().name(),
                    escapeCsv(outcome.getEmployerName()),
                    escapeCsv(outcome.getJobTitle()),
                    outcome.getStartDate() != null ? outcome.getStartDate().format(DATE_FORMATTER) : "",
                    outcome.getMonthlyAmount() != null ? outcome.getMonthlyAmount().doubleValue() : 0.0,
                    outcome.getEmploymentType() != null ? outcome.getEmploymentType().name() : ""
            );
        }

        writer.flush();
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * Exports survey responses to CSV.
     */
    public byte[] exportSurveyResponses(FacilitatorContext context, UUID surveyId) throws IOException {
        cohortIsolationService.getFacilitatorActiveCohort(context);
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Survey not found"));

        if (survey.getCohort() == null || !survey.getCohort().getId().equals(context.getCohortId())) {
            throw new RuntimeException("Access denied. Survey does not belong to your cohort.");
        }

        List<SurveyResponse> responses = surveyResponseRepository.findBySurveyId(surveyId);
        List<SurveyQuestion> questions = surveyQuestionRepository.findBySurveyIdOrderBySequenceOrder(surveyId);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.print("Participant Name,Email,Submitted At");
        for (SurveyQuestion question : questions) {
            writer.print("," + escapeCsv(question.getQuestionText()));
        }
        writer.println();

        // Write data rows
        for (SurveyResponse response : responses) {
            Participant participant = response.getParticipant();
            writer.printf("%s,%s,%s",
                    escapeCsv(participant.getFirstName() + " " + participant.getLastName()),
                    escapeCsv(participant.getEmail()),
                    response.getSubmittedAt() != null ? response.getSubmittedAt().atZone(java.time.ZoneId.systemDefault()).format(DATETIME_FORMATTER) : ""
            );

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
     * Calculates attendance percentage for an enrollment.
     */
    private BigDecimal calculateAttendancePercentage(Enrollment enrollment) {
        List<Attendance> attendances = attendanceRepository.findByEnrollmentId(enrollment.getId());
        if (attendances.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long presentCount = attendances.stream()
                .filter(a -> a.getStatus() == com.dseme.app.enums.AttendanceStatus.PRESENT ||
                           a.getStatus() == com.dseme.app.enums.AttendanceStatus.LATE ||
                           a.getStatus() == com.dseme.app.enums.AttendanceStatus.EXCUSED)
                .count();

        return BigDecimal.valueOf(presentCount)
                .divide(BigDecimal.valueOf(attendances.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

