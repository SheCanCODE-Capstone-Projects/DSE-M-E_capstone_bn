package com.dseme.app.repositories;

import com.dseme.app.models.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CenterRepository extends JpaRepository<Center, UUID> {
    boolean existsByIdAndPartner_PartnerId(UUID id, String partnerId);

    Optional<Center> findByIdAndPartner_PartnerId(UUID id, String partnerId);

}
