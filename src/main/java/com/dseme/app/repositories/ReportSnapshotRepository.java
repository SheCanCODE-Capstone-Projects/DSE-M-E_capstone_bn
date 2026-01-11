package com.dseme.app.repositories;

import com.dseme.app.models.ReportSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, UUID> {
    /**
     * Find report snapshots by partner ID.
     */
    List<ReportSnapshot> findByPartnerPartnerId(String partnerId);

    /**
     * Find report snapshot by partner ID, report type, and period.
     * Used to check if monthly report already exists.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT rs FROM ReportSnapshot rs WHERE rs.partner.partnerId = :partnerId " +
            "AND rs.reportType = :reportType " +
            "AND rs.reportPeriodStart = :periodStart " +
            "AND rs.reportPeriodEnd = :periodEnd"
    )
    Optional<ReportSnapshot> findByPartnerPartnerIdAndReportTypeAndPeriod(
            @org.springframework.data.repository.query.Param("partnerId") String partnerId,
            @org.springframework.data.repository.query.Param("reportType") String reportType,
            @org.springframework.data.repository.query.Param("periodStart") java.time.LocalDate periodStart,
            @org.springframework.data.repository.query.Param("periodEnd") java.time.LocalDate periodEnd
    );

    /**
     * Find report snapshots by partner ID and report type.
     */
    List<ReportSnapshot> findByPartnerPartnerIdAndReportType(String partnerId, String reportType);
}
