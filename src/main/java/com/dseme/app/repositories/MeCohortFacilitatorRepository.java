package com.dseme.app.repositories;

import com.dseme.app.models.MeCohortFacilitator;
import com.dseme.app.models.MeCohortFacilitatorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeCohortFacilitatorRepository extends JpaRepository<MeCohortFacilitator, MeCohortFacilitatorId> {

    List<MeCohortFacilitator> findByCohortId(UUID cohortId);

    List<MeCohortFacilitator> findByFacilitatorId(UUID facilitatorId);

    @Query("SELECT mcf.cohortId FROM MeCohortFacilitator mcf WHERE mcf.facilitatorId = :facilitatorId")
    List<UUID> findCohortIdsByFacilitatorId(@Param("facilitatorId") UUID facilitatorId);

    boolean existsByCohortIdAndFacilitatorId(UUID cohortId, UUID facilitatorId);

    void deleteByCohortIdAndFacilitatorId(UUID cohortId, UUID facilitatorId);
}
