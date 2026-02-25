package com.dseme.app.models;

import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
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
@Table(name = "users")

public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Forgotpassword forgotPassword;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "center_id")
    private Center center;

    @Column(name = "provider")
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        validateOrganizationRequirement();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        validateOrganizationRequirement();
    }
    
    /**
     * Validates that ME_OFFICER and FACILITATOR roles MUST have an organization.
     * This ensures no user can have these roles without being under an organization.
     */
    private void validateOrganizationRequirement() {
        if ((this.role == Role.ME_OFFICER || this.role == Role.FACILITATOR) && this.partner == null) {
            throw new IllegalStateException(
                "Cannot save user with role " + this.role + " without an organization. " +
                "ME Officers and Facilitators MUST be associated with an organization."
            );
        }
    }
}