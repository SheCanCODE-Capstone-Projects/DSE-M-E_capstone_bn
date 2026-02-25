package com.dseme.app.services.participants;

import com.dseme.app.dtos.me.ParticipantResponseDTO;
import com.dseme.app.dtos.participants.CreateParticipantDTO;
import com.dseme.app.dtos.participants.UpdateEmploymentDTO;
import com.dseme.app.dtos.participants.UpdateParticipantDTO;
import com.dseme.app.enums.ParticipantStatus;
import com.dseme.app.enums.Provider;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantManagementService {

    private final MeParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MeCohortRepository cohortRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public ParticipantResponseDTO addParticipant(CreateParticipantDTO dto) {
        User currentUser = getCurrentUser();
        
        MeCohort cohort = cohortRepository.findById(dto.getCohortId())
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));
        
        validateFacilitatorAccess(currentUser, cohort.getCourse());
        
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceAlreadyExistsException("User with email already exists");
        }
        
        String tempPassword = generateTempPassword();
        User user = User.builder()
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .passwordHash(encoder.encode(tempPassword))
                .role(Role.UNASSIGNED)
                .isActive(true)
                .isVerified(false)
                .provider(Provider.LOCAL)
                .partner(currentUser.getPartner())
                .center(currentUser.getCenter())
                .build();
        
        user = userRepository.save(user);
        
        MeParticipant participant = MeParticipant.builder()
                .user(user)
                .studentId(dto.getStudentId())
                .cohort(cohort)
                .status(ParticipantStatus.ENROLLED)
                .gender(dto.getGender())
                .build();
        
        participant = participantRepository.save(participant);
        
        return mapToResponseDTO(participant);
    }

    @Transactional
    public ParticipantResponseDTO updateParticipant(UUID participantId, UpdateParticipantDTO dto) {
        User currentUser = getCurrentUser();
        
        MeParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        validateFacilitatorAccess(currentUser, participant.getCohort().getCourse());
        
        User user = participant.getUser();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        userRepository.save(user);
        
        if (dto.getStudentId() != null) {
            participant.setStudentId(dto.getStudentId());
        }
        if (dto.getGender() != null) {
            participant.setGender(dto.getGender());
        }
        
        participant = participantRepository.save(participant);
        
        return mapToResponseDTO(participant);
    }

    @Transactional
    public ParticipantResponseDTO updateEmploymentStatus(UUID participantId, UpdateEmploymentDTO dto) {
        User currentUser = getCurrentUser();
        
        if (currentUser.getRole() != Role.ME_OFFICER) {
            throw new AccessDeniedException("Only ME Officers can update employment status");
        }
        
        MeParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        
        validateOrganizationAccess(currentUser, participant);
        
        participant.setEmploymentStatus(dto.getEmploymentStatus());
        participant.setAnnualIncome(dto.getAnnualIncome());
        
        participant = participantRepository.save(participant);
        
        return mapToResponseDTO(participant);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    private void validateFacilitatorAccess(User user, Course course) {
        if (user.getRole() != Role.FACILITATOR) {
            throw new AccessDeniedException("Only facilitators can perform this action");
        }
        
        Facilitator facilitator = facilitatorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator profile not found"));
        
        boolean isAssigned = courseAssignmentRepository
                .existsByFacilitatorIdAndCourseIdAndIsActive(facilitator.getId(), course.getId(), true);
        
        if (!isAssigned) {
            throw new AccessDeniedException("You are not assigned to this course");
        }
        
        if (course.getPartner() == null || user.getPartner() == null ||
            !course.getPartner().getPartnerId().equals(user.getPartner().getPartnerId())) {
            throw new AccessDeniedException("Course does not belong to your organization");
        }
    }
    
    private void validateOrganizationAccess(User user, MeParticipant participant) {
        if (user.getPartner() == null) {
            return;
        }
        
        if (participant.getUser().getPartner() == null ||
            !participant.getUser().getPartner().getPartnerId().equals(user.getPartner().getPartnerId())) {
            throw new AccessDeniedException("Participant does not belong to your organization");
        }
    }
    
    private String generateTempPassword() {
        int password = 100000 + random.nextInt(900000);
        return "Temp" + password;
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
                .build();
    }
}
