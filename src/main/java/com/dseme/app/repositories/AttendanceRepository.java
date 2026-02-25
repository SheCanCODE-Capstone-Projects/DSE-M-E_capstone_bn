package com.dseme.app.repositories;

import com.dseme.app.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    
    Optional<Attendance> findByParticipantIdAndSessionDate(
            UUID participantId,
            LocalDate sessionDate
    );

    boolean existsByParticipantIdAndSessionDate(
            UUID participantId,
            LocalDate sessionDate
    );

    @Query("SELECT a FROM Attendance a " +
           "WHERE a.participant.cohort.id = :cohortId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate")
    List<Attendance> findByCohortIdAndSessionDateBetween(
            @Param("cohortId") UUID cohortId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(a) FROM Attendance a " +
           "WHERE a.participant.cohort.id = :cohortId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate")
    Long countByCohortIdAndSessionDateBetween(
            @Param("cohortId") UUID cohortId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(a) FROM Attendance a " +
           "WHERE a.participant.cohort.id = :cohortId " +
           "AND a.sessionDate >= :startDate " +
           "AND a.sessionDate <= :endDate " +
           "AND a.status = 'PRESENT'")
    Long countPresentByCohortIdAndSessionDateBetween(
            @Param("cohortId") UUID cohortId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM Attendance a " +
           "WHERE a.participant.id = :participantId " +
           "AND (a.status = 'PRESENT' OR a.status = 'LATE' OR a.status = 'EXCUSED') " +
           "ORDER BY a.sessionDate DESC")
    List<Attendance> findByParticipantIdOrderBySessionDateDesc(@Param("participantId") UUID participantId);

    @Query("SELECT MAX(a.sessionDate) FROM Attendance a " +
           "WHERE a.participant.id = :participantId " +
           "AND (a.status = 'PRESENT' OR a.status = 'LATE' OR a.status = 'EXCUSED')")
    LocalDate findMostRecentAttendanceDateByParticipantId(@Param("participantId") UUID participantId);
}

