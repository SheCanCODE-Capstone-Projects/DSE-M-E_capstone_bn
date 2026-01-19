package com.dseme.app.repositories;

import com.dseme.app.models.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingModuleRepository extends JpaRepository<TrainingModule, UUID> {
    /**
     * Find training modules by program ID.
     * Used to get modules for a specific program.
     */
    @Query("SELECT tm FROM TrainingModule tm WHERE tm.program.id = :programId")
    List<TrainingModule> findByProgramId(@Param("programId") UUID programId);
}

