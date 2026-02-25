package com.dseme.app.models;

import com.dseme.app.enums.CohortStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cohort batch/intake (e.g. "She Can Code 2024 Cohort").
 * One batch can contain multiple course tracks (MeCohort), each with its own course + facilitator.
 */
@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "me_cohort_batches")
public class MeCohortBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "batch_id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Organization that owns this cohort batch.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    /**
     * Optional scoping to a center/branch (organization location).
     * Useful when ME Officers should see cohorts only for their assigned center.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id")
    private Center center;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CohortStatus status = CohortStatus.PLANNED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<MeCohort> tracks = new ArrayList<>();

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

