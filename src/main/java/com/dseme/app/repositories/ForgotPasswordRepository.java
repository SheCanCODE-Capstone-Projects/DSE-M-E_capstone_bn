package com.dseme.app.repositories;

import com.dseme.app.models.Forgotpassword;
import com.dseme.app.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ForgotPasswordRepository
        extends JpaRepository<Forgotpassword, Long> {

    Optional<Forgotpassword> findByToken(String token);
    Optional<Forgotpassword> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Forgotpassword f WHERE f.user = :user")
    void deleteByUser(@Param("user") User user);
}
