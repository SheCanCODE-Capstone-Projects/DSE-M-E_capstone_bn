package com.dseme.app.repositories;

import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndProvider(String email, Provider provider);
    
    /**
     * Count users by role and active status.
     * Used for M&E Officer dashboard statistics.
     */
    Long countByRoleAndIsActiveTrue(Role role);
}