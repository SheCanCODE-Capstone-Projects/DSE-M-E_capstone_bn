package com.dseme.app.services.meofficer;

import com.dseme.app.models.Partner;
import com.dseme.app.models.ReportSnapshot;
import com.dseme.app.models.User;
import com.dseme.app.repositories.PartnerRepository;
import com.dseme.app.repositories.ReportSnapshotRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Scheduled service for generating monthly partner reports.
 * 
 * Runs on the 1st day of each month at 2:00 AM.
 * Generates reports for all active partners and stores snapshots for audit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyReportScheduler {

    private final PartnerRepository partnerRepository;
    private final ReportSnapshotRepository reportSnapshotRepository;
    private final UserRepository userRepository;
    private final MEOfficerReportService reportService;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Scheduled job to generate monthly partner reports.
     * Runs on the 1st day of each month at 2:00 AM.
     * 
     * Cron expression: "0 0 2 1 * ?" = 2:00 AM on the 1st day of every month
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void generateMonthlyReports() {
        log.info("Starting monthly partner report generation...");

        LocalDate now = LocalDate.now();
        LocalDate periodStart = now.withDayOfMonth(1).minusMonths(1); // First day of previous month
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth()); // Last day of previous month
        String reportPeriod = periodStart.format(PERIOD_FORMATTER);

        // Get all active partners
        List<Partner> partners = partnerRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .collect(java.util.stream.Collectors.toList());

        log.info("Generating monthly reports for {} partners for period {}", partners.size(), reportPeriod);

        // Get system user or first ME_OFFICER for report generation
        User systemUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.dseme.app.enums.Role.ME_OFFICER)
                .findFirst()
                .orElse(null);

        if (systemUser == null) {
            log.error("No ME_OFFICER user found for report generation. Skipping monthly reports.");
            return;
        }

        for (Partner partner : partners) {
            try {
                generateMonthlyReportForPartner(partner, periodStart, periodEnd, reportPeriod, systemUser);
            } catch (Exception e) {
                log.error("Error generating monthly report for partner {}: {}", 
                        partner.getPartnerId(), e.getMessage(), e);
            }
        }

        log.info("Completed monthly partner report generation.");
    }

    /**
     * Generates monthly report for a specific partner.
     */
    private void generateMonthlyReportForPartner(
            Partner partner,
            LocalDate periodStart,
            LocalDate periodEnd,
            String reportPeriod,
            User systemUser
    ) {
        // Check if report already exists
        boolean reportExists = reportSnapshotRepository
                .findByPartnerPartnerIdAndReportTypeAndPeriod(
                        partner.getPartnerId(),
                        "MONTHLY_PARTNER_REPORT",
                        periodStart,
                        periodEnd
                )
                .isPresent();

        if (reportExists) {
            log.info("Monthly report for partner {} and period {} already exists. Skipping.", 
                    partner.getPartnerId(), reportPeriod);
            return;
        }

        try {
            // Create ME_OFFICER context for this partner
            // Note: We need a ME_OFFICER user assigned to this partner
            User meOfficer = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == com.dseme.app.enums.Role.ME_OFFICER &&
                               u.getPartner() != null &&
                               u.getPartner().getPartnerId().equals(partner.getPartnerId()))
                    .findFirst()
                    .orElse(systemUser);

            if (meOfficer.getPartner() == null || 
                !meOfficer.getPartner().getPartnerId().equals(partner.getPartnerId())) {
                log.warn("No ME_OFFICER assigned to partner {}. Using system user for report generation.", 
                        partner.getPartnerId());
            }

            // Generate comprehensive report
            com.dseme.app.dtos.meofficer.ExportReportRequestDTO request = 
                    com.dseme.app.dtos.meofficer.ExportReportRequestDTO.builder()
                            .reportType("COMPREHENSIVE")
                            .format("CSV")
                            .startDate(periodStart)
                            .endDate(periodEnd)
                            .build();

            com.dseme.app.dtos.meofficer.MEOfficerContext context = 
                    com.dseme.app.dtos.meofficer.MEOfficerContext.builder()
                            .meOfficer(meOfficer)
                            .partnerId(partner.getPartnerId())
                            .partner(partner)
                            .userId(meOfficer.getId())
                            .build();

            byte[] reportData = reportService.exportReport(context, request);

            // Store report snapshot
            ReportSnapshot snapshot = ReportSnapshot.builder()
                    .partner(partner)
                    .reportType("MONTHLY_PARTNER_REPORT")
                    .reportPeriodStart(periodStart)
                    .reportPeriodEnd(periodEnd)
                    .reportData(new String(reportData))
                    .fileFormat("CSV")
                    .fileSizeBytes((long) reportData.length)
                    .generatedBy(systemUser)
                    .build();

            reportSnapshotRepository.save(snapshot);

            log.info("Generated and stored monthly report for partner {} for period {}", 
                    partner.getPartnerId(), reportPeriod);

        } catch (Exception e) {
            log.error("Error generating monthly report for partner {}: {}", 
                    partner.getPartnerId(), e.getMessage(), e);
        }
    }
}
