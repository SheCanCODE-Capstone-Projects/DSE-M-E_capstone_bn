package com.dseme.app.repositories;

import com.dseme.app.enums.RequestStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.models.RoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoleRequestRepository extends JpaRepository<RoleRequest, UUID> {
    boolean existsByRequesterIdAndRequestedRoleAndPartnerPartnerIdAndCenterIdAndStatus(UUID requesterId, Role requestedRole, String partnerId, UUID centerId, RequestStatus status);
}
