package com.dseme.app.repositories;

import com.dseme.app.models.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingModuleRepository extends JpaRepository<TrainingModule, UUID> {
    /**
     * Find training modules by program ID.
     * Used to get modules for a specific program.
     */
    List<TrainingModule> findByProgramId(UUID programId);
}

