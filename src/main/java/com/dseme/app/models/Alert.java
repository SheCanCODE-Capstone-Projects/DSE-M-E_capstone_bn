package com.dseme.app.models;

import com.dseme.app.enums.AlertSeverity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for storing system alerts.
 * Used for reactive flagging of inconsistencies across the platform.
 */
@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alert_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient; // ME_OFFICER who should receive this alert

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType; // ATTENDANCE_CHECK, COMPLETION_CHECK, STATUS_MONITOR, etc.

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "issue_count", nullable = false)
    private Integer issueCount;

    @Column(name = "call_to_action", length = 100)
    private String callToAction; // Endpoint or action identifier

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType; // SURVEY, COHORT, PARTICIPANT, etc.

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @Builder.Default
    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
