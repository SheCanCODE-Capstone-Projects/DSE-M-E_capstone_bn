package com.dseme.app.repositories;

import com.dseme.app.models.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScoreRepository extends JpaRepository<Score, UUID> {
    List<Score> findByParticipantId(UUID participantId);
    
    List<Score> findByCourseId(UUID courseId);
    
    List<Score> findByParticipantIdAndCourseId(UUID participantId, UUID courseId);
    
    List<Score> findByAssignmentId(UUID assignmentId);
    
    Optional<Score> findByAssignmentIdAndParticipantId(UUID assignmentId, UUID participantId);
}

