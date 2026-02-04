package com.dseme.app.repositories;

import com.dseme.app.models.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs with filters for DONOR role.
     * Supports filtering by action, entity type, date range, and actor role.
     * Partner filtering is done through entity relationships.
     */
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:entityType IS NULL OR al.entityType = :entityType) AND " +
           "(:actorRole IS NULL OR al.actorRole = :actorRole) AND " +
           "(:dateRangeStart IS NULL OR al.createdAt >= :dateRangeStart) AND " +
           "(:dateRangeEnd IS NULL OR al.createdAt <= :dateRangeEnd) " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findWithFilters(
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("actorRole") String actorRole,
            @Param("dateRangeStart") Instant dateRangeStart,
            @Param("dateRangeEnd") Instant dateRangeEnd,
            Pageable pageable
    );
}
