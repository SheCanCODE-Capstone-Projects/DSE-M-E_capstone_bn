package com.dseme.app.repositories;

import com.dseme.app.enums.RequestStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.models.RoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, UUID> {
    boolean existsByRequesterIdAndRequestedRoleAndPartnerPartnerIdAndCenterIdAndStatus(UUID requesterId, Role requestedRole, String partnerId, UUID centerId, RequestStatus status);
    
    /**
     * Find pending role requests by partner ID.
     * Used to count pending access requests for dashboard.
     */
    @Query("SELECT rr FROM RoleRequest rr WHERE rr.partner.partnerId = :partnerId " +
           "AND rr.status = 'PENDING'")
    List<RoleRequest> findPendingByPartnerPartnerId(@Param("partnerId") String partnerId);
    
    /**
     * Count pending role requests by partner ID.
     */
    @Query("SELECT COUNT(rr) FROM RoleRequest rr WHERE rr.partner.partnerId = :partnerId " +
           "AND rr.status = 'PENDING'")
    long countPendingByPartnerPartnerId(@Param("partnerId") String partnerId);
}
