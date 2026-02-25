package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.ParticipantStatus;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service("facilitatorParticipantService")
@RequiredArgsConstructor
@Transactional
public class ParticipantManagementService {

    private final MeParticipantRepository participantRepository;
    private final MeCohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final CohortIsolationService cohortIsolationService;
    private final FacilitatorRepository facilitatorRepository;

    @Transactional(readOnly = true)
    public List<ParticipantDTO> getParticipantsByCohort(FacilitatorContext context) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        List<MeParticipant> participants = participantRepository.findByCohortId(cohort.getId());
        
        return participants.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantListItemDTO> getParticipantsListByCohort(FacilitatorContext context) {
        // Get facilitator from repository
        Facilitator facilitator = facilitatorRepository.findByUserId(context.getFacilitator().getId())
                .orElseThrow(() -> new RuntimeException("Facilitator not found"));
        
        // Get ALL cohorts for this facilitator
        List<MeCohort> cohorts = cohortRepository.findByFacilitatorId(facilitator.getId());
        
        // Get participants from ALL cohorts
        List<MeParticipant> allParticipants = new java.util.ArrayList<>();
        for (MeCohort cohort : cohorts) {
            allParticipants.addAll(participantRepository.findByCohortId(cohort.getId()));
        }
        
        return allParticipants.stream()
                .map(this::toListItemDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantDTO> getParticipantsByCohortId(FacilitatorContext context, UUID cohortId) {
        // Verify facilitator has access to this cohort
        MeCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Cohort not found"));
        
        // Get facilitator from repository
        Facilitator facilitator = facilitatorRepository.findByUserId(context.getFacilitator().getId())
                .orElseThrow(() -> new RuntimeException("Facilitator not found"));
        
        if (cohort.getFacilitator() == null || !cohort.getFacilitator().getId().equals(facilitator.getId())) {
            throw new RuntimeException("You don't have access to this cohort");
        }
        
        List<MeParticipant> participants = participantRepository.findByCohortId(cohortId);
        
        return participants.stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantListItemDTO> getParticipantsListByCohortId(FacilitatorContext context, UUID cohortId) {
        // Verify facilitator has access to this cohort
        MeCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Cohort not found"));
        
        // Get facilitator from repository
        Facilitator facilitator = facilitatorRepository.findByUserId(context.getFacilitator().getId())
                .orElseThrow(() -> new RuntimeException("Facilitator not found"));
        
        if (cohort.getFacilitator() == null || !cohort.getFacilitator().getId().equals(facilitator.getId())) {
            throw new RuntimeException("You don't have access to this cohort");
        }
        
        List<MeParticipant> participants = participantRepository.findByCohortId(cohortId);
        
        return participants.stream()
                .map(this::toListItemDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParticipantStatisticsDTO getStatistics(FacilitatorContext context) {
        // Get facilitator from repository
        Facilitator facilitator = facilitatorRepository.findByUserId(context.getFacilitator().getId())
                .orElseThrow(() -> new RuntimeException("Facilitator not found"));
        
        // Get ALL cohorts for this facilitator
        List<MeCohort> cohorts = cohortRepository.findByFacilitatorId(facilitator.getId());
        
        // Get participants from ALL cohorts
        List<MeParticipant> allParticipants = new java.util.ArrayList<>();
        for (MeCohort cohort : cohorts) {
            allParticipants.addAll(participantRepository.findByCohortId(cohort.getId()));
        }
        
        // Calculate gender distribution
        java.util.Map<String, Long> genderCounts = allParticipants.stream()
                .filter(p -> p.getGender() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        MeParticipant::getGender,
                        java.util.stream.Collectors.counting()
                ));
        
        return ParticipantStatisticsDTO.builder()
                .totalParticipantsCount((long) allParticipants.size())
                .activeParticipantsCount(allParticipants.stream()
                        .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE || 
                                   p.getStatus() == ParticipantStatus.ENROLLED ||
                                   p.getStatus() == ParticipantStatus.IN_PROGRESS)
                        .count())
                .inactiveParticipantsCount(allParticipants.stream()
                        .filter(p -> p.getStatus() == ParticipantStatus.DROPPED_OUT ||
                                   p.getStatus() == ParticipantStatus.WITHDRAWN)
                        .count())
                .genderDistribution(genderCounts.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                e -> e.getValue().intValue()
                        )))
                .build();
    }

    @Transactional(readOnly = true)
    public ParticipantDTO getParticipant(FacilitatorContext context, UUID participantId) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        MeParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));
        
        if (!participant.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Participant does not belong to your cohort");
        }
        
        return toDTO(participant);
    }

    public ParticipantDTO createParticipant(FacilitatorContext context, CreateParticipantRequest request) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        User user;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } else {
            user = createUserFromRequest(request);
        }
        
        MeParticipant participant = MeParticipant.builder()
                .user(user)
                .cohort(cohort)
                .status(ParticipantStatus.ENROLLED)
                .enrollmentDate(LocalDate.now())
                .createdBy(context.getFacilitator())
                .build();
        
        participant = participantRepository.save(participant);
        
        return toDTO(participant);
    }

    public ParticipantDTO updateParticipant(FacilitatorContext context, UUID participantId, UpdateParticipantRequest request) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        MeParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));
        
        if (!participant.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Participant does not belong to your cohort");
        }
        
        if (request.getFirstName() != null) {
            participant.getUser().setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            participant.getUser().setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            participant.getUser().setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            participant.setStatus(request.getStatus());
        }
        if (request.getCompletionDate() != null) {
            participant.setCompletionDate(request.getCompletionDate());
        }
        if (request.getDropoutDate() != null) {
            participant.setDropoutDate(request.getDropoutDate());
        }
        if (request.getDropoutReason() != null) {
            participant.setDropoutReason(request.getDropoutReason());
        }
        
        participant = participantRepository.save(participant);
        
        return toDTO(participant);
    }

    public void deleteParticipant(FacilitatorContext context, UUID participantId) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        MeParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));
        
        if (!participant.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Participant does not belong to your cohort");
        }
        
        participantRepository.delete(participant);
    }

    private User createUserFromRequest(CreateParticipantRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPasswordHash("TEMP_PASSWORD");
        user.setRole(com.dseme.app.enums.Role.PARTICIPANT);
        
        return userRepository.save(user);
    }

    private ParticipantDTO toDTO(MeParticipant participant) {
        return ParticipantDTO.builder()
                .id(participant.getId())
                .userId(participant.getUser() != null ? participant.getUser().getId() : null)
                .firstName(participant.getUser() != null ? participant.getUser().getFirstName() : null)
                .lastName(participant.getUser() != null ? participant.getUser().getLastName() : null)
                .email(participant.getUser() != null ? participant.getUser().getEmail() : null)
                .phoneNumber(null)
                .cohortId(participant.getCohort() != null ? participant.getCohort().getId() : null)
                .cohortName(participant.getCohort() != null ? participant.getCohort().getName() : null)
                .courseId(participant.getCohort() != null && participant.getCohort().getCourse() != null ? participant.getCohort().getCourse().getId() : null)
                .courseName(participant.getCohort() != null && participant.getCohort().getCourse() != null ? participant.getCohort().getCourse().getName() : null)
                .status(participant.getStatus())
                .enrollmentDate(participant.getEnrollmentDate())
                .completionDate(participant.getCompletionDate())
                .dropoutDate(participant.getDropoutDate())
                .dropoutReason(participant.getDropoutReason())
                .isVerified(participant.getIsVerified())
                .verifiedBy(participant.getVerifiedBy() != null ? participant.getVerifiedBy().getEmail() : null)
                .createdBy(participant.getCreatedBy() != null ? participant.getCreatedBy().getEmail() : null)
                .build();
    }

    private ParticipantListItemDTO toListItemDTO(MeParticipant participant) {
        return ParticipantListItemDTO.builder()
                .participantId(participant.getId() != null ? participant.getId().toString() : null)
                .firstName(participant.getUser() != null ? participant.getUser().getFirstName() : null)
                .lastName(participant.getUser() != null ? participant.getUser().getLastName() : null)
                .email(participant.getUser() != null ? participant.getUser().getEmail() : null)
                .phone(null)
                .gender(participant.getGender())
                .enrollmentDate(participant.getEnrollmentDate())
                .attendancePercentage(null)
                .enrollmentStatus(participant.getStatus() != null ? participant.getStatus().name() : null)
                .enrollmentId(participant.getId() != null ? participant.getId().toString() : null)
                .build();
    }
}
