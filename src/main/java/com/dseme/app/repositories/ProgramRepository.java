package com.dseme.app.repositories;

import com.dseme.app.models.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgramRepository extends JpaRepository<Program, UUID> {
    /**
     * Find programs by partner ID.
     * Used for partner-level program queries.
     */
    List<Program> findByPartnerPartnerId(String partnerId);
    
    /**
     * Count programs by partner ID.
     * Used for partner metrics.
     */
    long countByPartnerPartnerId(String partnerId);
}
