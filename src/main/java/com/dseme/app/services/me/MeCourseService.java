package com.dseme.app.services.me;

import com.dseme.app.dtos.me.*;
import com.dseme.app.enums.CourseLevel;
import com.dseme.app.enums.CourseStatus;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeCourseService {
    
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final MeCohortRepository cohortRepository;
    private final UserRepository userRepository;

    public Page<CourseResponseDTO> getAllCourses(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        // Filter courses by organization
        if (currentUser.getPartner() != null) {
            return courseRepository.findByPartner_PartnerId(currentUser.getPartner().getPartnerId(), pageable)
                    .map(this::mapToResponseDTO);
        }
        
        // Super admin sees all courses
        return courseRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    public CourseResponseDTO getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return mapToResponseDTO(course);
    }

    @Transactional
    public CourseResponseDTO createCourse(CreateCourseDTO dto) {
        User currentUser = getCurrentUser();
        
        // Check for duplicate code within the same organization
        if (currentUser.getPartner() != null) {
            if (courseRepository.findByCodeAndPartner_PartnerId(dto.getCode(), currentUser.getPartner().getPartnerId()).isPresent()) {
                throw new ResourceAlreadyExistsException("Course with code already exists in your organization");
            }
        } else {
            if (courseRepository.findByCode(dto.getCode()).isPresent()) {
                throw new ResourceAlreadyExistsException("Course with code already exists");
            }
        }

        Course course = Course.builder()
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .level(CourseLevel.valueOf(dto.getLevel().toUpperCase()))
                .durationWeeks(dto.getDurationWeeks() != null ? dto.getDurationWeeks() : 12)
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 30)
                .status(CourseStatus.ACTIVE)
                .partner(currentUser.getPartner())
                .build();

        course = courseRepository.save(course);
        return mapToResponseDTO(course);
    }

    @Transactional
    public CourseResponseDTO updateCourse(UUID id, CreateCourseDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        // Verify course belongs to user's organization
        validateCourseAccess(course);

        course.setName(dto.getName());
        course.setDescription(dto.getDescription());
        course.setLevel(CourseLevel.valueOf(dto.getLevel().toUpperCase()));
        course.setDurationWeeks(dto.getDurationWeeks());
        course.setMaxParticipants(dto.getMaxParticipants());

        course = courseRepository.save(course);
        return mapToResponseDTO(course);
    }

    @Transactional
    public void deleteCourse(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        // Verify course belongs to user's organization
        validateCourseAccess(course);
        
        courseRepository.delete(course);
    }

    @Transactional
    public CourseResponseDTO toggleCourseStatus(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        // Verify course belongs to user's organization
        validateCourseAccess(course);
        
        course.setStatus(course.getStatus() == CourseStatus.ACTIVE 
                ? CourseStatus.INACTIVE 
                : CourseStatus.ACTIVE);
        
        course = courseRepository.save(course);
        return mapToResponseDTO(course);
    }

    public List<ParticipantResponseDTO> getCourseParticipants(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        return cohortRepository.findByCourseId(courseId)
                .stream()
                .flatMap(cohort -> cohort.getParticipants().stream())
                .map(this::mapParticipantToResponseDTO)
                .collect(Collectors.toList());
    }

    private CourseResponseDTO mapToResponseDTO(Course course) {
        List<FacilitatorSummaryDTO> facilitators = courseAssignmentRepository.findActiveByCourseId(course.getId())
                .stream()
                .map(assignment -> mapToFacilitatorSummaryDTO(assignment.getFacilitator()))
                .collect(Collectors.toList());

        int currentParticipants = cohortRepository.findByCourseId(course.getId())
                .stream()
                .mapToInt(cohort -> cohort.getParticipants().size())
                .sum();

        return CourseResponseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .description(course.getDescription())
                .level(course.getLevel().name())
                .durationWeeks(course.getDurationWeeks())
                .maxParticipants(course.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .status(course.getStatus().name())
                .facilitators(facilitators)
                .facilitatorsCount(facilitators.size())
                .participantsCount(currentParticipants)
                .build();
    }

    private FacilitatorSummaryDTO mapToFacilitatorSummaryDTO(Facilitator facilitator) {
        return FacilitatorSummaryDTO.builder()
                .id(facilitator.getId())
                .firstName(facilitator.getUser().getFirstName())
                .lastName(facilitator.getUser().getLastName())
                .build();
    }

    private ParticipantResponseDTO mapParticipantToResponseDTO(MeParticipant participant) {
        return ParticipantResponseDTO.builder()
                .id(participant.getId())
                .firstName(participant.getUser().getFirstName())
                .lastName(participant.getUser().getLastName())
                .email(participant.getUser().getEmail())
                .studentId(participant.getStudentId())
                .enrollmentDate(participant.getEnrollmentDate())
                .status(participant.getStatus().name())
                .score(participant.getScore())
                .cohort(CohortSummaryDTO.builder()
                        .id(participant.getCohort().getId())
                        .name(participant.getCohort().getName())
                        .course(CourseSummaryDTO.builder()
                                .id(participant.getCohort().getCourse().getId())
                                .name(participant.getCohort().getCourse().getName())
                                .code(participant.getCohort().getCourse().getCode())
                                .build())
                        .build())
                .build();
    }
    
    private User getCurrentUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    private void validateCourseAccess(Course course) {
        User currentUser = getCurrentUser();
        
        // Super admin can access all courses
        if (currentUser.getPartner() == null) {
            return;
        }
        
        // Check if course belongs to user's organization
        if (course.getPartner() == null || 
            !course.getPartner().getPartnerId().equals(currentUser.getPartner().getPartnerId())) {
            throw new com.dseme.app.exceptions.AccessDeniedException(
                "You can only access courses from your organization");
        }
    }
}