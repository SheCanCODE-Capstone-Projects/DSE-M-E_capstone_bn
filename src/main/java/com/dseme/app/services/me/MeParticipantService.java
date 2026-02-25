package com.dseme.app.services.me;

import com.dseme.app.dtos.me.*;
import com.dseme.app.dtos.participants.CreateParticipantDTO;
import com.dseme.app.enums.ParticipantStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.MeCohort;
import com.dseme.app.models.MeParticipant;
import com.dseme.app.models.User;
import com.dseme.app.repositories.MeCohortRepository;
import com.dseme.app.repositories.MeParticipantRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeParticipantService {

    private final MeParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MeCohortRepository cohortRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<ParticipantResponseDTO> getAllParticipants(UUID cohortId, UUID batchId, String status, Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<MeParticipant> participants;

        if (cohortId != null) {
            participants = participantRepository.findByCohortId(cohortId, pageable);
        } else if (batchId != null) {
            participants = participantRepository.findByCohort_Batch_Id(batchId, pageable);
        } else if (status != null) {
            participants = participantRepository.findByStatus(ParticipantStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            participants = participantRepository.findAll(pageable);
        }

        List<ParticipantResponseDTO> filtered = participants.getContent().stream()
                .filter(p -> belongsToSameOrganization(p, currentUser))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    public ParticipantResponseDTO getParticipantById(UUID id) {
        User currentUser = getCurrentUser();
        MeParticipant participant = participantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        if (!belongsToSameOrganization(participant, currentUser)) {
            throw new ResourceNotFoundException("Participant not found");
        }
        
        return mapToResponseDTO(participant);
    }

    public Map<String, Object> getParticipantStats() {
        User currentUser = getCurrentUser();
        Map<String, Object> stats = new HashMap<>();
        
        long totalParticipants = participantRepository.findAll().stream()
                .filter(p -> belongsToSameOrganization(p, currentUser))
                .count();
        
        stats.put("totalParticipants", totalParticipants);
        stats.put("enrolledCount", participantRepository.countByStatus(ParticipantStatus.ENROLLED));
        stats.put("activeCount", participantRepository.countByStatus(ParticipantStatus.IN_PROGRESS));
        stats.put("completedCount", participantRepository.countByStatus(ParticipantStatus.COMPLETED));
        stats.put("droppedCount", participantRepository.countByStatus(ParticipantStatus.DROPPED));
        return stats;
    }

    @Transactional
    public ParticipantResponseDTO createParticipant(CreateParticipantDTO dto) {
        User currentUser = getCurrentUser();
        
        // Check if email already exists
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Check if student ID already exists
        if (participantRepository.findByStudentId(dto.getStudentId()).isPresent()) {
            throw new IllegalArgumentException("Student ID already exists");
        }
        
        // Verify cohort exists
        MeCohort cohort = cohortRepository.findById(dto.getCohortId())
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        
        // Create user account - participants don't require partner
        User user = User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode("DSE@2024"))
                .role(Role.PARTICIPANT)
                .isActive(true)
                .build();
        
        // Set partner if current user has one
        if (currentUser.getPartner() != null) {
            user.setPartner(currentUser.getPartner());
        }
        
        user = userRepository.save(user);
        
        // Create participant
        MeParticipant participant = MeParticipant.builder()
                .user(user)
                .studentId(dto.getStudentId())
                .cohort(cohort)
                .enrollmentDate(LocalDate.now())
                .status(ParticipantStatus.ENROLLED)
                .gender(dto.getGender())
                .createdBy(currentUser)
                .build();
        participant = participantRepository.save(participant);
        
        return mapToResponseDTO(participant);
    }

    private boolean belongsToSameOrganization(MeParticipant participant, User currentUser) {
        // If current user has no partner, show all participants
        if (currentUser.getPartner() == null) {
            return true;
        }
        
        // Check if participant's cohort batch has a center with matching partner
        if (participant.getCohort().getBatch() != null && 
            participant.getCohort().getBatch().getCenter() != null &&
            participant.getCohort().getBatch().getCenter().getPartner() != null) {
            return participant.getCohort().getBatch().getCenter().getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        // If batch has no center, check if participant user has matching partner
        if (participant.getUser().getPartner() != null) {
            return participant.getUser().getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        // If no partner info, show to all users with partner (same org)
        return true;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ParticipantResponseDTO mapToResponseDTO(MeParticipant participant) {
        return ParticipantResponseDTO.builder()
                .id(participant.getId())
                .firstName(participant.getUser().getFirstName())
                .lastName(participant.getUser().getLastName())
                .email(participant.getUser().getEmail())
                .studentId(participant.getStudentId())
                .enrollmentDate(participant.getEnrollmentDate())
                .status(participant.getStatus().name())
                .score(participant.getScore())
                .employmentStatus(participant.getEmploymentStatus())
                .annualIncome(participant.getAnnualIncome())
                .gender(participant.getGender())
                .cohort(CohortSummaryDTO.builder()
                        .id(participant.getCohort().getId())
                        .name(participant.getCohort().getName())
                        .course(CourseSummaryDTO.builder()
                                .id(participant.getCohort().getCourse().getId())
                                .name(participant.getCohort().getCourse().getName())
                                .code(participant.getCohort().getCourse().getCode())
                                .level(participant.getCohort().getCourse().getLevel().name())
                                .build())
                        .facilitator(participant.getCohort().getFacilitator() != null ?
                                FacilitatorSummaryDTO.builder()
                                        .id(participant.getCohort().getFacilitator().getId())
                                        .firstName(participant.getCohort().getFacilitator().getUser().getFirstName())
                                        .lastName(participant.getCohort().getFacilitator().getUser().getLastName())
                                        .build() : null)
                        .build())
                .build();
    }
}
