package com.dseme.app.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "me_cohort_facilitators")
@IdClass(MeCohortFacilitatorId.class)
public class MeCohortFacilitator {

    @Id
    @Column(name = "cohort_id")
    private UUID cohortId;

    @Id
    @Column(name = "facilitator_id")
    private UUID facilitatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", insertable = false, updatable = false)
    private MeCohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facilitator_id", insertable = false, updatable = false)
    private Facilitator facilitator;

    @Builder.Default
    @Column(name = "role", length = 50)
    private String role = "FACILITATOR";

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @PrePersist
    protected void onCreate() {
        this.assignedAt = Instant.now();
    }
}
