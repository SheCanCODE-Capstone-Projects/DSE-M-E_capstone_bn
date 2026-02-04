package com.dseme.app.repositories;

import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndProvider(String email, Provider provider);
    
    /**
     * Find all facilitators by partner ID.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.partner.partnerId = :partnerId")
    List<User> findByRoleAndPartnerPartnerId(@Param("role") Role role, @Param("partnerId") String partnerId);
    
    /**
     * Find facilitator by ID and partner ID.
     * Used to ensure partner-level isolation.
     */
    @Query("SELECT u FROM User u WHERE u.id = :facilitatorId AND u.role = :role AND u.partner.partnerId = :partnerId")
    Optional<User> findFacilitatorByIdAndPartnerPartnerId(
            @Param("facilitatorId") UUID facilitatorId,
            @Param("role") Role role,
            @Param("partnerId") String partnerId
    );
    
    /**
     * Find all users by role.
     * Used for DONOR notification and dashboard services.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") Role role);
}
