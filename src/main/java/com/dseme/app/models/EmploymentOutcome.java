package com.dseme.app.models;

import com.dseme.app.enums.EmploymentStatus;
import com.dseme.app.enums.EmploymentType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "employment_outcomes")
public class EmploymentOutcome {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "employment_outcome_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internship_id")
    private Internship internship;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus;

    @Column(name = "employer_name", length = 255)
    private String employerName;

    @Column(name = "job_title", length = 255)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 50)
    private EmploymentType employmentType;

    @Column(name = "salary_range", length = 50)
    private String salaryRange;
    
    /**
     * Monthly salary or stipend amount.
     * Stored as decimal for precise currency calculations.
     */
    @Column(name = "monthly_amount", precision = 10, scale = 2)
    private java.math.BigDecimal monthlyAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Builder.Default
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

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

