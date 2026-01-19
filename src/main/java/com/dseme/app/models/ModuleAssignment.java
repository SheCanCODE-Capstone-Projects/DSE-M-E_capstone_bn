package com.dseme.app.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents the assignment of a training module to a facilitator.
 * 
 * ME_OFFICER assigns modules to facilitators.
 * Facilitators can only view and manage participants for their assigned modules.
 */
@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "module_assignments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"facilitator_id", "module_id", "cohort_id"})
})
public class ModuleAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id")
    private UUID id;

    /**
     * Facilitator assigned to this module.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facilitator_id", nullable = false)
    private User facilitator;

    /**
     * Training module assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private TrainingModule module;

    /**
     * Cohort this assignment is for.
     * Module must belong to an active cohort.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    /**
     * ME_OFFICER who assigned this module.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    /**
     * When the assignment was created.
     */
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = Instant.now();
        }
    }
}
