package com.dseme.app.repositories;

import com.dseme.app.models.Course;
import com.dseme.app.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {
    
    Optional<Course> findByCode(String code);
    
    List<Course> findByStatus(CourseStatus status);
    
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Course c WHERE c.name LIKE %:name% OR c.code LIKE %:name%")
    Page<Course> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT COUNT(c) FROM Course c WHERE c.status = :status")
    long countByStatus(@Param("status") CourseStatus status);
}