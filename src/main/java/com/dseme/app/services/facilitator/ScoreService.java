package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.UploadScoreDTO;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Cohort;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Score;
import com.dseme.app.models.TrainingModule;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ScoreRepository;
import com.dseme.app.repositories.TrainingModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final EnrollmentRepository enrollmentRepository;
    private final TrainingModuleRepository trainingModuleRepository;
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
    public List<Score> uploadScores(FacilitatorContext context, UploadScoreDTO dto) {
        // Validate facilitator has active cohort
        Cohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        // Validate cohort is active
        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot upload scores for a cohort with status: " + activeCohort.getStatus() +
                ". Only ACTIVE cohorts allow score uploads."
            );
        }

        List<Score> scores = new ArrayList<>();

        for (UploadScoreDTO.ScoreRecord record : dto.getRecords()) {
            // Load enrollment
            Enrollment enrollment = enrollmentRepository.findById(record.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment not found with ID: " + record.getEnrollmentId()
                    ));

            // Validate enrollment belongs to facilitator's active cohort
            if (!enrollment.getCohort().getId().equals(context.getCohortId())) {
                throw new AccessDeniedException(
                    "Access denied. Enrollment does not belong to your assigned active cohort."
                );
            }

            // Validate enrollment's cohort belongs to facilitator's center
            if (!enrollment.getCohort().getCenter().getId().equals(context.getCenterId())) {
                throw new AccessDeniedException(
                    "Access denied. Enrollment's cohort does not belong to your assigned center."
                );
            }

            // Load module
            TrainingModule module = trainingModuleRepository.findById(record.getModuleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Training module not found with ID: " + record.getModuleId()
                    ));

            // Validate module belongs to facilitator's active cohort's program
            if (!module.getProgram().getId().equals(activeCohort.getProgram().getId())) {
                throw new AccessDeniedException(
                    "Access denied. Module does not belong to your active cohort's program."
                );
            }

            // Validate score value is within range (0-100)
            // This is also enforced by @DecimalMin/@DecimalMax in DTO and model
            if (record.getScoreValue().compareTo(java.math.BigDecimal.ZERO) < 0 ||
                record.getScoreValue().compareTo(new java.math.BigDecimal("100.0")) > 0) {
                throw new AccessDeniedException(
                    "Score value must be between 0 and 100. Provided value: " + record.getScoreValue()
                );
            }

            // Create score record
            Score score = Score.builder()
                    .enrollment(enrollment)
                    .module(module)
                    .assessmentType(record.getAssessmentType())
                    .scoreValue(record.getScoreValue())
                    .recordedBy(context.getFacilitator()) // Audit: who recorded the score
                    .recordedAt(Instant.now()) // When the score was recorded
                    .build();

            // Save score
            scores.add(scoreRepository.save(score));
        }

        return scores;
    }

    /**
     * Uploads a single score record.
     * Convenience method for single score upload.
     * 
     * @param context Facilitator context
     * @param enrollmentId Enrollment ID
     * @param moduleId Module ID
     * @param assessmentType Assessment type
     * @param scoreValue Score value (0-100)
     * @return Created Score entity
     */
    public Score uploadSingleScore(
            FacilitatorContext context,
            UUID enrollmentId,
            UUID moduleId,
            com.dseme.app.enums.AssessmentType assessmentType,
            java.math.BigDecimal scoreValue
    ) {
        UploadScoreDTO dto = UploadScoreDTO.builder()
                .records(List.of(
                        UploadScoreDTO.ScoreRecord.builder()
                                .enrollmentId(enrollmentId)
                                .moduleId(moduleId)
                                .assessmentType(assessmentType)
                                .scoreValue(scoreValue)
                                .build()
                ))
                .build();

        List<Score> scores = uploadScores(context, dto);
        return scores.get(0);
    }
}

