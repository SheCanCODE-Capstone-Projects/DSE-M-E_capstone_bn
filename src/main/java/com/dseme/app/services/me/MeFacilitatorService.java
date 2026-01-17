package com.dseme.app.services.me;

import com.dseme.app.dtos.me.*;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeFacilitatorService {
    
    private final FacilitatorRepository facilitatorRepository;
    private final UserRepository userRepository;
    private final CourseAssignmentRepository courseAssignmentRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<FacilitatorResponseDTO> getAllFacilitators(Pageable pageable) {
        return facilitatorRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    public FacilitatorResponseDTO getFacilitatorById(UUID id) {
        Facilitator facilitator = facilitatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        return mapToResponseDTO(facilitator);
    }

    @Transactional
    public FacilitatorResponseDTO createFacilitator(CreateFacilitatorDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("User with email already exists");
        }

        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .role(Role.FACILITATOR)
                .isActive(true)
                .isVerified(true)
                .build();
        
        user = userRepository.save(user);

        Facilitator facilitator = Facilitator.builder()
                .user(user)
                .employeeId(dto.getEmployeeId())
                .department(dto.getDepartment())
                .hireDate(dto.getHireDate())
                .specialization(dto.getSpecialization())
                .build();

        facilitator = facilitatorRepository.save(facilitator);
        return mapToResponseDTO(facilitator);
    }

    @Transactional
    public FacilitatorResponseDTO updateFacilitator(UUID id, CreateFacilitatorDTO dto) {
        Facilitator facilitator = facilitatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));

        User user = facilitator.getUser();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        
        facilitator.setEmployeeId(dto.getEmployeeId());
        facilitator.setDepartment(dto.getDepartment());
        facilitator.setHireDate(dto.getHireDate());
        facilitator.setSpecialization(dto.getSpecialization());

        facilitator = facilitatorRepository.save(facilitator);
        return mapToResponseDTO(facilitator);
    }

    @Transactional
    public void deleteFacilitator(UUID id) {
        Facilitator facilitator = facilitatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        facilitatorRepository.delete(facilitator);
    }

    public List<AssignedCourseDTO> getFacilitatorCourses(UUID facilitatorId) {
        Facilitator facilitator = facilitatorRepository.findById(facilitatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        
        return courseAssignmentRepository.findActiveByfacilitatorId(facilitatorId)
                .stream()
                .map(assignment -> mapToCourseDTO(assignment.getCourse()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignCourse(UUID facilitatorId, AssignCourseDTO dto) {
        Facilitator facilitator = facilitatorRepository.findById(facilitatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (courseAssignmentRepository.findByFacilitatorAndCourse(facilitator, course).isPresent()) {
            throw new ResourceAlreadyExistsException("Course already assigned to facilitator");
        }

        CourseAssignment assignment = CourseAssignment.builder()
                .facilitator(facilitator)
                .course(course)
                .build();

        courseAssignmentRepository.save(assignment);
    }

    @Transactional
    public void removeCourseAssignment(UUID facilitatorId, UUID courseId) {
        Facilitator facilitator = facilitatorRepository.findById(facilitatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        CourseAssignment assignment = courseAssignmentRepository.findByFacilitatorAndCourse(facilitator, course)
                .orElseThrow(() -> new ResourceNotFoundException("Course assignment not found"));

        courseAssignmentRepository.delete(assignment);
    }

    private FacilitatorResponseDTO mapToResponseDTO(Facilitator facilitator) {
        List<AssignedCourseDTO> assignedCourses = facilitator.getCourseAssignments()
                .stream()
                .filter(CourseAssignment::getIsActive)
                .map(assignment -> mapToCourseDTO(assignment.getCourse()))
                .collect(Collectors.toList());

        return FacilitatorResponseDTO.builder()
                .id(facilitator.getId())
                .firstName(facilitator.getUser().getFirstName())
                .lastName(facilitator.getUser().getLastName())
                .email(facilitator.getUser().getEmail())
                .employeeId(facilitator.getEmployeeId())
                .department(facilitator.getDepartment())
                .specialization(facilitator.getSpecialization())
                .status(facilitator.getUser().getIsActive() ? "ACTIVE" : "INACTIVE")
                .assignedCourses(assignedCourses)
                .build();
    }

    private AssignedCourseDTO mapToCourseDTO(Course course) {
        return AssignedCourseDTO.builder()
                .id(course.getId())
                .name(course.getName())
                .code(course.getCode())
                .level(course.getLevel().name())
                .build();
    }
}