package com.dseme.app.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Model for storing report snapshots for audit purposes.
 * Used for scheduled monthly partner reports.
 */
@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "report_snapshots")
public class ReportSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "snapshot_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // MONTHLY_PARTNER_REPORT, QUARTERLY_PARTNER_REPORT, ANNUAL_PARTNER_REPORT, CUSTOM

    @Column(name = "report_period_start", nullable = false)
    private LocalDate reportPeriodStart;

    @Column(name = "report_period_end", nullable = false)
    private LocalDate reportPeriodEnd;

    @Column(name = "report_data", nullable = false, columnDefinition = "TEXT")
    private String reportData; // JSON or CSV data stored as text

    @Column(name = "file_format", nullable = false, length = 10)
    private String fileFormat; // CSV, PDF, JSON

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
