package com.dseme.app.repositories;

import com.dseme.app.models.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByCohortId(UUID cohortId);
    List<Assignment> findByCourseId(UUID courseId);
    List<Assignment> findByCohortIdAndCourseId(UUID cohortId, UUID courseId);
}
