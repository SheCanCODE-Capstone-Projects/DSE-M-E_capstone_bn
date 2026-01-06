package com.dseme.app.repositories;

import com.dseme.app.models.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    /**
     * Find participant by email.
     * Used to check if participant already exists.
     */
    Optional<Participant> findByEmail(String email);
}

