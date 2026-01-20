package com.dseme.app.repositories;

import com.dseme.app.models.CourseAssignment;
import com.dseme.app.models.Course;
import com.dseme.app.models.Facilitator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, UUID> {
    
    List<CourseAssignment> findByFacilitator(Facilitator facilitator);
    
    List<CourseAssignment> findByCourse(Course course);
    
    List<CourseAssignment> findByIsActive(Boolean isActive);
    
    Optional<CourseAssignment> findByFacilitatorAndCourse(Facilitator facilitator, Course course);
    
    @Query("SELECT ca FROM CourseAssignment ca WHERE ca.facilitator.id = :facilitatorId AND ca.isActive = true")
    List<CourseAssignment> findActiveByfacilitatorId(@Param("facilitatorId") UUID facilitatorId);
    
    @Query("SELECT ca FROM CourseAssignment ca WHERE ca.course.id = :courseId AND ca.isActive = true")
    List<CourseAssignment> findActiveByCourseId(@Param("courseId") UUID courseId);
}