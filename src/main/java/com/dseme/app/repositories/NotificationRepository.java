package com.dseme.app.repositories;

import com.dseme.app.models.Notification;
import com.dseme.app.models.RoleRequest;
import com.dseme.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRoleRequestId(UUID roleRequestId);

    Optional<Notification> findByRoleRequestAndRecipient(RoleRequest roleRequest, User recipient);

    List<Notification> findByRecipient(User recipient);
}
