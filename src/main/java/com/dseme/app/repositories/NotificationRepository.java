package com.dseme.app.repositories;

import com.dseme.app.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRoleRequestId(UUID roleRequestId);

}
