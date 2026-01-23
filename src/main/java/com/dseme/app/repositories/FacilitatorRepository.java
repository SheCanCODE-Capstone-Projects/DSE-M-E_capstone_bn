package com.dseme.app.repositories;

import com.dseme.app.models.Facilitator;
import com.dseme.app.models.User;
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
public interface FacilitatorRepository extends JpaRepository<Facilitator, UUID> {
    
    Optional<Facilitator> findByUser(User user);
    
    Optional<Facilitator> findByEmployeeId(String employeeId);
    
    @Query("SELECT f FROM Facilitator f WHERE f.user.firstName LIKE %:name% OR f.user.lastName LIKE %:name% OR f.employeeId LIKE %:name%")
    Page<Facilitator> findByNameOrEmployeeId(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT f FROM Facilitator f WHERE f.department = :department")
    List<Facilitator> findByDepartment(@Param("department") String department);
    
    @Query("SELECT COUNT(f) FROM Facilitator f WHERE f.user.isActive = true")
    long countActiveFacilitators();
}