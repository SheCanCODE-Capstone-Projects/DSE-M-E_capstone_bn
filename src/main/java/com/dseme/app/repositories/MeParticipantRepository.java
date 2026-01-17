package com.dseme.app.repositories;

import com.dseme.app.models.MeParticipant;
import com.dseme.app.models.MeCohort;
import com.dseme.app.models.User;
import com.dseme.app.enums.ParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MeParticipantRepository extends JpaRepository<MeParticipant, UUID> {
    
    Optional<MeParticipant> findByUser(User user);
    
    Optional<MeParticipant> findByStudentId(String studentId);
    
    List<MeParticipant> findByCohort(MeCohort cohort);
    
    List<MeParticipant> findByStatus(ParticipantStatus status);
    
    @Query("SELECT p FROM MeParticipant p WHERE p.cohort.id = :cohortId")
    List<MeParticipant> findByCohortId(@Param("cohortId") UUID cohortId);
    
    @Query("SELECT p FROM MeParticipant p WHERE p.user.firstName LIKE %:name% OR p.user.lastName LIKE %:name% OR p.studentId LIKE %:name%")
    Page<MeParticipant> findByNameOrStudentId(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM MeParticipant p WHERE p.status = :status")
    long countByStatus(@Param("status") ParticipantStatus status);
    
    @Query("SELECT AVG(p.score) FROM MeParticipant p WHERE p.score IS NOT NULL")
    BigDecimal findAverageScore();
    
    @Query("SELECT COUNT(p) FROM MeParticipant p")
    long countTotalParticipants();
}