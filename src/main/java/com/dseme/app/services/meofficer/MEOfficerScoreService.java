package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.dtos.meofficer.ScoreValidationResponseDTO;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Score;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.ScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for ME_OFFICER score validation operations.
 * 
 * Enforces strict partner-level data isolation.
 * ME_OFFICER can validate scores but CANNOT edit score values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerScoreService {

    private final ScoreRepository scoreRepository;
    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Validates a score uploaded by a facilitator.
     * ME_OFFICER validates but does NOT edit score values.
     * 
     * @param context ME_OFFICER context
     * @param scoreId Score ID
     * @return Validation response
     * @throws ResourceNotFoundException if score not found
     * @throws AccessDeniedException if score doesn't belong to ME_OFFICER's partner or is already validated
     */
    @Transactional
    public ScoreValidationResponseDTO validateScore(
            MEOfficerContext context,
            UUID scoreId
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Load score with partner validation
        Score score = scoreRepository
                .findByIdAndEnrollmentParticipantPartnerPartnerId(scoreId, context.getPartnerId())
                .orElseThrow(() -> {
                    // Check if score exists but belongs to different partner
                    Score s = scoreRepository.findById(scoreId).orElse(null);
                    if (s != null && !s.getEnrollment().getParticipant().getPartner().getPartnerId().equals(context.getPartnerId())) {
                        throw new AccessDeniedException(
                                "Access denied. Score does not belong to your assigned partner."
                        );
                    }
                    return new ResourceNotFoundException(
                            "Score not found with ID: " + scoreId
                    );
                });

        // Check if already validated
        if (Boolean.TRUE.equals(score.getIsValidated())) {
            throw new AccessDeniedException(
                    "Score is already validated."
            );
        }

        // Validate score (DO NOT modify score values - read-only validation)
        score.setIsValidated(true);
        score.setValidatedBy(context.getMeOfficer());
        score.setValidatedAt(Instant.now());
        score = scoreRepository.save(score);

        // Create audit log entry
        AuditLog auditLog = AuditLog.builder()
                .actor(context.getMeOfficer())
                .actorRole("ME_OFFICER")
                .action("VALIDATE_SCORE")
                .entityType("SCORE")
                .entityId(score.getId())
                .description(String.format(
                        "ME_OFFICER %s (%s) validated score for participant %s %s in module %s (Score ID: %s, Value: %s/%s)",
                        context.getMeOfficer().getFirstName(),
                        context.getMeOfficer().getEmail(),
                        score.getEnrollment().getParticipant().getFirstName(),
                        score.getEnrollment().getParticipant().getLastName(),
                        score.getModule().getModuleName(),
                        score.getId(),
                        score.getScoreValue(),
                        score.getMaxScore()
                ))
                .build();
        auditLogRepository.save(auditLog);

        log.info("ME_OFFICER {} validated score {}", 
                context.getMeOfficer().getEmail(), scoreId);

        // Build response (includes score details for confirmation, but values are read-only)
        return ScoreValidationResponseDTO.builder()
                .scoreId(score.getId())
                .isValidated(true)
                .validatedByName(context.getMeOfficer().getFirstName() + " " + 
                               context.getMeOfficer().getLastName())
                .validatedByEmail(context.getMeOfficer().getEmail())
                .validatedAt(score.getValidatedAt())
                .enrollmentId(score.getEnrollment().getId())
                .moduleId(score.getModule().getId())
                .moduleName(score.getModule().getModuleName())
                .assessmentType(score.getAssessmentType())
                .assessmentName(score.getAssessmentName())
                .scoreValue(score.getScoreValue()) // Read-only - for confirmation only
                .maxScore(score.getMaxScore()) // Read-only - for confirmation only
                .assessmentDate(score.getAssessmentDate())
                .message("Score validated successfully. Score values remain unchanged.")
                .build();
    }
}
