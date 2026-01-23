package com.dseme.app.repositories;

import com.dseme.app.models.AccessRequest;
import com.dseme.app.enums.RequestStatus;
import com.dseme.app.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, UUID> {
    
    List<AccessRequest> findByStatus(RequestStatus status);
    
    Page<AccessRequest> findByStatus(RequestStatus status, Pageable pageable);
    
    List<AccessRequest> findByRequestedRole(Role requestedRole);
    
    List<AccessRequest> findByRequesterEmail(String requesterEmail);
    
    @Query("SELECT COUNT(ar) FROM AccessRequest ar WHERE ar.status = :status")
    long countByStatus(@Param("status") RequestStatus status);
    
    @Query("SELECT ar FROM AccessRequest ar WHERE ar.requesterName LIKE %:name% OR ar.requesterEmail LIKE %:name%")
    Page<AccessRequest> findByRequesterNameOrEmail(@Param("name") String name, Pageable pageable);
}