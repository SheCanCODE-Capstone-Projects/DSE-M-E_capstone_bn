package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.GradeResponseDTO;
import com.dseme.app.dtos.facilitator.UploadScoreDTO;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.MeCohort;
import com.dseme.app.models.MeParticipant;
import com.dseme.app.models.Score;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.AssignmentRepository;
import com.dseme.app.repositories.MeParticipantRepository;
import com.dseme.app.repositories.ScoreRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for uploading scores by facilitators.
 * 
 * This service enforces:
 * - Participant must be enrolled
 * - Participant must belong to active cohort
 * - Module must belong to facilitator's active cohort's program
 * - Score ranges enforced (0-100)
 * - Numeric validation
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final MeParticipantRepository participantRepository;
    private final TrainingModuleRepository trainingModuleRepository;
    private final AssignmentRepository assignmentRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Uploads scores for one or more participants (batch support).
     * 
     * Rules:
     * 1. Participant must be enrolled (enrollment must exist)
     * 2. Enrollment must belong to facilitator's active cohort
     * 3. Module must belong to facilitator's active cohort's program
     * 4. Score value must be between 0 and 100 (enforced by validation and DB constraint)
     * 5. Numeric validation (BigDecimal)
     * 
     * @param context Facilitator context
     * @param dto Score data (single or batch)
     * @return List of created Score entities
     * @throws ResourceNotFoundException if enrollment or module not found
     * @throws AccessDeniedException if validation fails
     */
    public List<GradeResponseDTO> uploadScores(FacilitatorContext context, UploadScoreDTO dto) {
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot upload scores for a cohort with status: " + activeCohort.getStatus()
            );
        }

        List<Score> scores = new ArrayList<>();

        for (UploadScoreDTO.ScoreRecord record : dto.getRecords()) {
            MeParticipant participant = participantRepository.findById(record.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Participant not found with ID: " + record.getEnrollmentId()
                    ));

            if (!participant.getCohort().getId().equals(context.getCohortId())) {
                throw new AccessDeniedException(
                    "Access denied. Participant does not belong to your assigned active cohort."
                );
            }

            com.dseme.app.models.Assignment assignment = assignmentRepository.findById(record.getAssignmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with ID: " + record.getAssignmentId()
                    ));

            if (!assignment.getCohort().getId().equals(context.getCohortId())) {
                throw new AccessDeniedException(
                    "Access denied. Assignment does not belong to your cohort."
                );
            }

            if (record.getScoreValue().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                record.getScoreValue().compareTo(new java.math.BigDecimal(assignment.getMaxScore().toString())) > 0) {
                throw new AccessDeniedException(
                    "Score value must be between 0 and " + assignment.getMaxScore() + ". Provided value: " + record.getScoreValue()
                );
            }

            Score score = Score.builder()
                    .participant(participant)
                    .module(assignment.getModule())
                    .assignment(assignment)
                    .assessmentType(assignment.getType())
                    .assessmentName(assignment.getTitle())
                    .scoreValue(record.getScoreValue())
                    .recordedBy(context.getFacilitator())
                    .recordedAt(Instant.now())
                    .build();

            scores.add(scoreRepository.save(score));
        }

        return scores.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<GradeResponseDTO> getParticipantScoresDTO(UUID participantId, FacilitatorContext context) {
        return getParticipantScores(participantId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<GradeResponseDTO> getModuleScoresDTO(UUID moduleId, FacilitatorContext context) {
        return getModuleScores(moduleId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<GradeResponseDTO> getParticipantModuleScoresDTO(UUID participantId, UUID moduleId, FacilitatorContext context) {
        return getParticipantModuleScores(participantId, moduleId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<GradeResponseDTO> getAssignmentScoresDTO(UUID assignmentId, FacilitatorContext context) {
        com.dseme.app.models.Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
        
        return assignment.getScores().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private GradeResponseDTO mapToDTO(Score score) {
        String participantName = score.getParticipant().getUser().getFirstName() + " " + 
                                 score.getParticipant().getUser().getLastName();
        String recordedByName = score.getRecordedBy().getFirstName() + " " + 
                                score.getRecordedBy().getLastName();
        
        return GradeResponseDTO.builder()
                .scoreId(score.getId())
                .participantId(score.getParticipant().getId())
                .participantName(participantName)
                .moduleId(score.getModule().getId())
                .moduleName(score.getModule().getModuleName())
                .assessmentType(score.getAssessmentType())
                .assessmentName(score.getAssessmentName())
                .scoreValue(score.getScoreValue())
                .recordedByName(recordedByName)
                .recordedAt(score.getRecordedAt())
                .build();
    }

    /**
     * Get all scores for a specific participant.
     * 
     * @param participantId Participant ID
     * @return List of scores for the participant
     */
    public List<Score> getParticipantScores(UUID participantId) {
        return scoreRepository.findByParticipantId(participantId);
    }

    /**
     * Get all scores for a specific module.
     * 
     * @param moduleId Module ID
     * @return List of scores for the module
     */
    public List<Score> getModuleScores(UUID moduleId) {
        return scoreRepository.findByModuleId(moduleId);
    }

    /**
     * Get scores for a specific participant in a specific module.
     * 
     * @param participantId Participant ID
     * @param moduleId Module ID
     * @return List of scores for the participant in the module
     */
    public List<Score> getParticipantModuleScores(UUID participantId, UUID moduleId) {
        return scoreRepository.findByParticipantIdAndModuleId(participantId, moduleId);
    }
}

