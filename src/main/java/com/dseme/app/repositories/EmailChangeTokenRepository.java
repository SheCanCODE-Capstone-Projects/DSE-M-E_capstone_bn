package com.dseme.app.repositories;

import com.dseme.app.models.EmailChangeToken;
import com.dseme.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    Optional<EmailChangeToken> findByToken(String token);
    void deleteByUser(User user);
}
