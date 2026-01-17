package com.dseme.app.repositories;

import com.dseme.app.models.MeCohort;
import com.dseme.app.models.Course;
import com.dseme.app.models.Facilitator;
import com.dseme.app.enums.CohortStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeCohortRepository extends JpaRepository<MeCohort, UUID> {
    
    List<MeCohort> findByCourse(Course course);
    
    List<MeCohort> findByFacilitator(Facilitator facilitator);
    
    List<MeCohort> findByStatus(CohortStatus status);
    
    Page<MeCohort> findByStatus(CohortStatus status, Pageable pageable);
    
    @Query("SELECT c FROM MeCohort c WHERE c.name LIKE %:name%")
    Page<MeCohort> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM MeCohort c WHERE c.status = :status")
    long countByStatus(@Param("status") CohortStatus status);
    
    @Query("SELECT c FROM MeCohort c WHERE c.course.id = :courseId")
    List<MeCohort> findByCourseId(@Param("courseId") UUID courseId);
}