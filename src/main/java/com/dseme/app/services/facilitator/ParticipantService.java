package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.CreateParticipantDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.ParticipantResponseDTO;
import com.dseme.app.dtos.facilitator.UpdateParticipantDTO;
import com.dseme.app.enums.EnrollmentStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import com.dseme.app.repositories.AttendanceRepository;
import com.dseme.app.repositories.EnrollmentRepository;
import com.dseme.app.repositories.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing participants by facilitators.
 * 
 * This service enforces:
 * - Participants can only be created in facilitator's active cohort
 * - Participants must belong to facilitator's partner
 * - Participants must have no existing enrollment
 * - Enrollment status is set to ENROLLED
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final CohortIsolationService cohortIsolationService;

    /**
     * Converts a Participant entity to ParticipantResponseDTO.
     * Extracts meaningful data without circular references or sensitive information.
     * 
     * @param participant Participant entity
     * @return ParticipantResponseDTO
     */
    private ParticipantResponseDTO toResponseDTO(Participant participant) {
        return ParticipantResponseDTO.builder()
                .id(participant.getId())
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .dateOfBirth(participant.getDateOfBirth())
                .gender(participant.getGender())
                .disabilityStatus(participant.getDisabilityStatus())
                .educationLevel(participant.getEducationLevel())
                .employmentStatusBaseline(participant.getEmploymentStatusBaseline())
                .partnerId(participant.getPartner() != null ? participant.getPartner().getPartnerId() : null)
                .partnerName(participant.getPartner() != null ? participant.getPartner().getPartnerName() : null)
                .createdByName(participant.getCreatedBy() != null ? 
                    (participant.getCreatedBy().getFirstName() != null && participant.getCreatedBy().getLastName() != null ?
                        participant.getCreatedBy().getFirstName() + " " + participant.getCreatedBy().getLastName() :
                        participant.getCreatedBy().getEmail()) : null)
                .createdByEmail(participant.getCreatedBy() != null ? participant.getCreatedBy().getEmail() : null)
                .createdAt(participant.getCreatedAt())
                .updatedAt(participant.getUpdatedAt())
                .build();
    }

    /**
     * Creates a new participant profile and enrolls them in the facilitator's active cohort.
     * 
     * Rules:
     * 1. Participant must not already exist (by email)
     * 2. Participant must not have any existing enrollment
     * 3. Participant is assigned to facilitator's partner
     * 4. Participant is enrolled in facilitator's active cohort
     * 5. Enrollment status is set to ENROLLED
     * 6. Enrollment is_verified is set to false (facilitator cannot set verification flags)
     * 
     * @param context Facilitator context
     * @param dto Participant creation data
     * @return Created ParticipantResponseDTO
     * @throws ResourceAlreadyExistsException if participant with email already exists
     * @throws AccessDeniedException if validation fails
     */
    public ParticipantResponseDTO createParticipant(FacilitatorContext context, CreateParticipantDTO dto) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Check if participant with email already exists
        if (participantRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                "Participant with email '" + dto.getEmail() + "' already exists"
            );
        }

        // Create participant
        Participant participant = Participant.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .disabilityStatus(dto.getDisabilityStatus())
                .educationLevel(dto.getEducationLevel())
                .employmentStatusBaseline(dto.getEmploymentStatusBaseline())
                .partner(context.getPartner()) // Must belong to facilitator's partner
                .createdBy(context.getFacilitator()) // Audit: who created the participant
                .build();

        Participant savedParticipant = participantRepository.save(participant);

        // Create enrollment in facilitator's active cohort
        Enrollment enrollment = Enrollment.builder()
                .participant(savedParticipant)
                .cohort(context.getCohort()) // Must be facilitator's active cohort
                .enrollmentDate(LocalDate.now())
                .status(EnrollmentStatus.ENROLLED) // Status must be ENROLLED
                .isVerified(false) // Facilitator cannot set verification flags
                .verifiedBy(null) // Verification must be done by authorized user
                .createdBy(context.getFacilitator()) // Audit: who created the enrollment
                .build();

        enrollmentRepository.save(enrollment);

        // Return DTO to avoid circular references and sensitive data
        return toResponseDTO(savedParticipant);
    }

    /**
     * Validates that a participant can be created by the facilitator.
     * 
     * @param context Facilitator context
     * @param email Participant email to check
     * @throws ResourceAlreadyExistsException if participant already exists
     * @throws AccessDeniedException if participant has existing enrollment
     */
    public void validateParticipantCreation(FacilitatorContext context, String email) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Check if participant exists
        Participant existingParticipant = participantRepository.findByEmail(email)
                .orElse(null);

        if (existingParticipant != null) {
            // Check if participant has any enrollment
            if (enrollmentRepository.existsByParticipantId(existingParticipant.getId())) {
                throw new ResourceAlreadyExistsException(
                    "Participant with email '" + email + "' already has an enrollment"
                );
            }

            // Check if participant belongs to facilitator's partner
            if (!existingParticipant.getPartner().getPartnerId().equals(context.getPartnerId())) {
                throw new AccessDeniedException(
                    "Access denied. Participant belongs to a different partner."
                );
            }
        }
    }

    /**
     * Updates a participant profile.
     * 
     * Rules:
     * 1. Participant must exist
     * 2. Participant must belong to facilitator's cohort (via enrollment)
     * 3. Participant must belong to facilitator's center (via cohort)
     * 4. Only editable fields can be updated: firstName, lastName, gender, disabilityStatus, dateOfBirth
     * 5. Immutable fields are enforced: partner, cohort, status, verification flags
     * 
     * @param context Facilitator context
     * @param participantId Participant ID to update
     * @param dto Update data (only editable fields)
     * @return Updated ParticipantResponseDTO
     * @throws ResourceNotFoundException if participant not found
     * @throws AccessDeniedException if participant doesn't belong to facilitator's cohort/center
     */
    public ParticipantResponseDTO updateParticipant(
            FacilitatorContext context,
            UUID participantId,
            UpdateParticipantDTO dto
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load participant
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Participant not found with ID: " + participantId
                ));

        // Validate participant belongs to facilitator's partner
        if (!participant.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned partner."
            );
        }

        // Validate participant has enrollment in facilitator's active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participantId);
        boolean belongsToActiveCohort = enrollments.stream()
                .anyMatch(enrollment -> 
                    enrollment.getCohort().getId().equals(context.getCohortId()) &&
                    enrollment.getCohort().getCenter().getId().equals(context.getCenterId())
                );

        if (!belongsToActiveCohort) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned active cohort."
            );
        }

        // Update only editable fields
        participant.setFirstName(dto.getFirstName());
        participant.setLastName(dto.getLastName());
        participant.setDateOfBirth(dto.getDateOfBirth());
        participant.setGender(dto.getGender());
        participant.setDisabilityStatus(dto.getDisabilityStatus());

        // Note: Immutable fields are NOT updated:
        // - partner (enforced by validation above)
        // - cohort (enforced by enrollment validation)
        // - email, phone, educationLevel, employmentStatusBaseline (not in DTO)
        // - status (enrollment status, not participant status)
        // - verification flags (enrollment.isVerified, enrollment.verifiedBy)

        Participant savedParticipant = participantRepository.save(participant);
        
        // Return DTO to avoid circular references and sensitive data
        return toResponseDTO(savedParticipant);
    }

    /**
     * Gets a participant by ID, ensuring it belongs to facilitator's cohort and center.
     * 
     * @param context Facilitator context
     * @param participantId Participant ID
     * @return ParticipantResponseDTO
     * @throws ResourceNotFoundException if participant not found
     * @throws AccessDeniedException if participant doesn't belong to facilitator's scope
     */
    public ParticipantResponseDTO getParticipantById(FacilitatorContext context, UUID participantId) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load participant
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Participant not found with ID: " + participantId
                ));

        // Validate participant belongs to facilitator's partner
        if (!participant.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned partner."
            );
        }

        // Validate participant has enrollment in facilitator's active cohort
        List<Enrollment> enrollments = enrollmentRepository.findByParticipantId(participantId);
        boolean belongsToActiveCohort = enrollments.stream()
                .anyMatch(enrollment -> 
                    enrollment.getCohort().getId().equals(context.getCohortId()) &&
                    enrollment.getCohort().getCenter().getId().equals(context.getCenterId())
                );

        if (!belongsToActiveCohort) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned active cohort."
            );
        }

        // Return DTO to avoid circular references and sensitive data
        return toResponseDTO(participant);
    }

    /**
     * Gets detailed participant information by ID.
     * 
     * @param context Facilitator context
     * @param participantId Participant ID
     * @return ParticipantDetailDTO
     * @throws ResourceNotFoundException if participant not found
     * @throws AccessDeniedException if participant doesn't belong to facilitator's scope
     */
    public com.dseme.app.dtos.facilitator.ParticipantDetailDTO getParticipantDetail(
            FacilitatorContext context, 
            UUID participantId
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load participant
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Participant not found with ID: " + participantId
                ));

        // Validate participant belongs to facilitator's partner
        if (!participant.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Participant does not belong to your assigned partner."
            );
        }

        // Get enrollment for facilitator's active cohort
        Enrollment enrollment = enrollmentRepository.findByParticipantId(participantId).stream()
                .filter(e -> e.getCohort().getId().equals(context.getCohortId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                    "Access denied. Participant is not enrolled in your active cohort."
                ));

        // Calculate attendance percentage
        java.math.BigDecimal attendancePercentage = calculateAttendancePercentage(enrollment);

        // Get display status
        String displayStatus = getDisplayStatus(enrollment);

        return com.dseme.app.dtos.facilitator.ParticipantDetailDTO.builder()
                .participantId(participant.getId())
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .gender(participant.getGender())
                .disabilityStatus(participant.getDisabilityStatus())
                .cohortName(enrollment.getCohort().getCohortName())
                .enrollmentStatus(displayStatus)
                .attendancePercentage(attendancePercentage)
                .enrollmentId(enrollment.getId())
                .cohortId(enrollment.getCohort().getId())
                .build();
    }

    /**
     * Calculates attendance percentage for an enrollment.
     */
    private java.math.BigDecimal calculateAttendancePercentage(Enrollment enrollment) {
        java.util.List<com.dseme.app.models.Attendance> allAttendances = enrollment.getAttendances();
        
        if (allAttendances.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }

        long presentCount = allAttendances.stream()
                .filter(a -> a.getStatus() == com.dseme.app.enums.AttendanceStatus.PRESENT || 
                            a.getStatus() == com.dseme.app.enums.AttendanceStatus.LATE || 
                            a.getStatus() == com.dseme.app.enums.AttendanceStatus.EXCUSED)
                .count();

        return java.math.BigDecimal.valueOf(presentCount)
                .divide(java.math.BigDecimal.valueOf(allAttendances.size()), 4, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Gets display status for enrollment.
     * Returns "INACTIVE" for ENROLLED status after 2-week gap, otherwise returns status name.
     */
    private String getDisplayStatus(Enrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            // Check if there's a 2-week gap (meaning they were ACTIVE before)
            java.time.LocalDate mostRecentAttendance = attendanceRepository
                    .findMostRecentAttendanceDateByEnrollmentId(enrollment.getId());
            
            if (mostRecentAttendance != null) {
                long daysSinceLastAttendance = java.time.temporal.ChronoUnit.DAYS.between(
                        mostRecentAttendance, java.time.LocalDate.now());
                if (daysSinceLastAttendance >= 14) {
                    return "INACTIVE";
                }
            }
        }
        
        return enrollment.getStatus().name();
    }

    /**
     * Updates enrollment status manually (for DROPPED_OUT, WITHDRAWN).
     * 
     * @param context Facilitator context
     * @param enrollmentId Enrollment ID
     * @param dto Status update DTO
     * @return Updated enrollment
     * @throws ResourceNotFoundException if enrollment not found
     * @throws AccessDeniedException if enrollment doesn't belong to facilitator's scope
     */
    public Enrollment updateEnrollmentStatus(
            FacilitatorContext context,
            UUID enrollmentId,
            com.dseme.app.dtos.facilitator.UpdateEnrollmentStatusDTO dto
    ) {
        // Validate facilitator has active cohort
        cohortIsolationService.getFacilitatorActiveCohort(context);

        // Load enrollment
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Enrollment not found with ID: " + enrollmentId
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

        // Only allow DROPPED_OUT and WITHDRAWN status changes by facilitator
        if (dto.getStatus() != EnrollmentStatus.DROPPED_OUT && 
            dto.getStatus() != EnrollmentStatus.WITHDRAWN) {
            throw new AccessDeniedException(
                "Access denied. Facilitators can only set status to DROPPED_OUT or WITHDRAWN."
            );
        }

        // Update status
        enrollment.setStatus(dto.getStatus());

        // Set dropout date and reason if status is DROPPED_OUT
        if (dto.getStatus() == EnrollmentStatus.DROPPED_OUT) {
            enrollment.setDropoutDate(java.time.LocalDate.now());
            enrollment.setDropoutReason(dto.getReason());
        }

        return enrollmentRepository.save(enrollment);
    }
}

