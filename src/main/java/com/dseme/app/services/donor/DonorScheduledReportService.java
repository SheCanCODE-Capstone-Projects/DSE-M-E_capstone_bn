package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.dtos.donor.ExportReportRequestDTO;
import com.dseme.app.models.ReportSnapshot;
import com.dseme.app.models.User;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.services.donor.DonorReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Service for automated scheduled report generation for DONOR role.
 * 
 * Generates:
 * - Daily reports (if configured)
 * - Weekly reports (every Monday)
 * - Monthly reports (first day of month)
 * 
 * Reports are stored as snapshots and can be emailed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonorScheduledReportService {

    private final DonorReportService reportService;
    private final DonorAuthorizationService authorizationService;
    private final UserRepository userRepository;

    /**
     * Generates weekly portfolio report every Monday at 8:00 AM.
     * 
     * Cron: 0 0 8 * * MON (Every Monday at 8:00 AM)
     */
    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional
    public void generateWeeklyReport() {
        log.info("Starting scheduled weekly portfolio report generation...");

        try {
            // Get the first DONOR user (or system user) to generate report
            User donorUser = findDonorUser();
            if (donorUser == null) {
                log.warn("No DONOR user found. Skipping weekly report generation.");
                return;
            }

            DonorContext context = authorizationService.loadDonorContext(donorUser.getEmail());

            // Calculate week period (last 7 days)
            LocalDate periodEnd = LocalDate.now().minusDays(1);
            LocalDate periodStart = periodEnd.minusDays(6);

            // Generate comprehensive report
            ExportReportRequestDTO request = ExportReportRequestDTO.builder()
                    .reportType("COMPREHENSIVE")
                    .format("PDF")
                    .dateRangeStart(periodStart)
                    .dateRangeEnd(periodEnd)
                    .build();

            byte[] reportData = reportService.exportReport(context, request);

            // Store snapshot
            ReportSnapshot snapshot = reportService.createReportSnapshot(
                    context,
                    "WEEKLY_PORTFOLIO_REPORT",
                    periodStart,
                    periodEnd,
                    reportData,
                    "PDF"
            );

            log.info("Weekly portfolio report generated successfully. Snapshot ID: {}", snapshot.getId());

            // TODO: Send email notification to DONOR users
            // emailService.sendReportEmail(context, snapshot);

        } catch (Exception e) {
            log.error("Error generating weekly portfolio report: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates monthly portfolio report on the first day of each month at 9:00 AM.
     * 
     * Cron: 0 0 9 1 * * (First day of month at 9:00 AM)
     */
    @Scheduled(cron = "0 0 9 1 * *")
    @Transactional
    public void generateMonthlyReport() {
        log.info("Starting scheduled monthly portfolio report generation...");

        try {
            // Get the first DONOR user (or system user) to generate report
            User donorUser = findDonorUser();
            if (donorUser == null) {
                log.warn("No DONOR user found. Skipping monthly report generation.");
                return;
            }

            DonorContext context = authorizationService.loadDonorContext(donorUser.getEmail());

            // Calculate month period (previous month)
            LocalDate periodEnd = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).minusDays(1);
            LocalDate periodStart = periodEnd.with(TemporalAdjusters.firstDayOfMonth());

            // Generate comprehensive report
            ExportReportRequestDTO request = ExportReportRequestDTO.builder()
                    .reportType("COMPREHENSIVE")
                    .format("PDF")
                    .dateRangeStart(periodStart)
                    .dateRangeEnd(periodEnd)
                    .build();

            byte[] reportData = reportService.exportReport(context, request);

            // Store snapshot
            ReportSnapshot snapshot = reportService.createReportSnapshot(
                    context,
                    "MONTHLY_PORTFOLIO_REPORT",
                    periodStart,
                    periodEnd,
                    reportData,
                    "PDF"
            );

            log.info("Monthly portfolio report generated successfully. Snapshot ID: {}", snapshot.getId());

            // TODO: Send email notification to DONOR users
            // emailService.sendReportEmail(context, snapshot);

        } catch (Exception e) {
            log.error("Error generating monthly portfolio report: {}", e.getMessage(), e);
        }
    }

    /**
     * Generates quarterly portfolio report on the first day of each quarter at 10:00 AM.
     * 
     * Cron: 0 0 10 1 1,4,7,10 * (First day of Jan, Apr, Jul, Oct at 10:00 AM)
     */
    @Scheduled(cron = "0 0 10 1 1,4,7,10 *")
    @Transactional
    public void generateQuarterlyReport() {
        log.info("Starting scheduled quarterly portfolio report generation...");

        try {
            // Get the first DONOR user (or system user) to generate report
            User donorUser = findDonorUser();
            if (donorUser == null) {
                log.warn("No DONOR user found. Skipping quarterly report generation.");
                return;
            }

            DonorContext context = authorizationService.loadDonorContext(donorUser.getEmail());

            // Calculate quarter period (previous quarter)
            LocalDate now = LocalDate.now();
            LocalDate periodEnd = now.with(TemporalAdjusters.firstDayOfMonth()).minusDays(1);
            
            // Determine quarter start (previous quarter)
            int periodEndMonth = periodEnd.getMonthValue();
            int quarterStartMonth = ((periodEndMonth - 1) / 3) * 3 + 1; // 1, 4, 7, or 10
            LocalDate periodStart = periodEnd.withMonth(quarterStartMonth).with(TemporalAdjusters.firstDayOfMonth());

            // Generate comprehensive report
            ExportReportRequestDTO request = ExportReportRequestDTO.builder()
                    .reportType("COMPREHENSIVE")
                    .format("PDF")
                    .dateRangeStart(periodStart)
                    .dateRangeEnd(periodEnd)
                    .build();

            byte[] reportData = reportService.exportReport(context, request);

            // Store snapshot
            ReportSnapshot snapshot = reportService.createReportSnapshot(
                    context,
                    "QUARTERLY_PORTFOLIO_REPORT",
                    periodStart,
                    periodEnd,
                    reportData,
                    "PDF"
            );

            log.info("Quarterly portfolio report generated successfully. Snapshot ID: {}", snapshot.getId());

            // TODO: Send email notification to DONOR users
            // emailService.sendReportEmail(context, snapshot);

        } catch (Exception e) {
            log.error("Error generating quarterly portfolio report: {}", e.getMessage(), e);
        }
    }

    /**
     * Finds a DONOR user to use for report generation.
     * Returns the first active DONOR user found.
     */
    private User findDonorUser() {
        List<User> donorUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.dseme.app.enums.Role.DONOR &&
                           Boolean.TRUE.equals(u.getIsActive()) &&
                           Boolean.TRUE.equals(u.getIsVerified()))
                .limit(1)
                .toList();

        return donorUsers.isEmpty() ? null : donorUsers.get(0);
    }
}
