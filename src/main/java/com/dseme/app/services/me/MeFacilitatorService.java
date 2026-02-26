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
    private final PartnerRepository partnerRepository;
    private final CenterRepository centerRepository;
    private final MeCohortBatchRepository cohortBatchRepository;
    private final MeCohortRepository cohortRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<FacilitatorResponseDTO> getAllFacilitators(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Facilitator> facilitators = facilitatorRepository.findAll(pageable);
        
        // If user has no partner, show all facilitators (admin/donor view)
        if (currentUser.getPartner() == null) {
            return facilitators.map(this::mapToResponseDTO);
        }
        
        // Filter by organization
        List<FacilitatorResponseDTO> filtered = facilitators.getContent().stream()
                .filter(f -> f.getUser().getPartner() != null && 
                            f.getUser().getPartner().getPartnerId().equals(currentUser.getPartner().getPartnerId()))
                .map(this::mapToResponseDTO)
                .collect(java.util.stream.Collectors.toList());
        
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
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

        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        
        Center center = centerRepository.findById(dto.getCenterId())
                .orElseThrow(() -> new ResourceNotFoundException("Center not found"));

        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .role(Role.FACILITATOR)
                .partner(partner)
                .center(center)
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

        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        
        Center center = centerRepository.findById(dto.getCenterId())
                .orElseThrow(() -> new ResourceNotFoundException("Center not found"));

        User user = facilitator.getUser();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPartner(partner);
        user.setCenter(center);
        
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

    public List<PartnerDTO> getAllPartners() {
        return partnerRepository.findAll()
                .stream()
                .filter(Partner::getIsActive)
                .map(partner -> PartnerDTO.builder()
                        .partnerId(partner.getPartnerId())
                        .partnerName(partner.getPartnerName())
                        .country(partner.getCountry())
                        .region(partner.getRegion())
                        .build())
                .collect(Collectors.toList());
    }

    public List<CenterDTO> getAllCenters(String partnerId) {
        List<Center> centers = partnerId != null 
                ? centerRepository.findAll().stream()
                        .filter(c -> c.getPartner().getPartnerId().equals(partnerId))
                        .collect(Collectors.toList())
                : centerRepository.findAll();
        
        return centers.stream()
                .filter(Center::getIsActive)
                .map(center -> CenterDTO.builder()
                        .centerId(center.getId())
                        .centerName(center.getCenterName())
                        .location(center.getLocation())
                        .partnerId(center.getPartner().getPartnerId())
                        .partnerName(center.getPartner().getPartnerName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public FacilitatorResponseDTO assignOrganization(UUID facilitatorId, AssignOrganizationDTO dto) {
        Facilitator facilitator = facilitatorRepository.findById(facilitatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));

        Partner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        
        Center center = centerRepository.findById(dto.getCenterId())
                .orElseThrow(() -> new ResourceNotFoundException("Center not found"));

        User user = facilitator.getUser();
        user.setPartner(partner);
        user.setCenter(center);
        userRepository.save(user);

        return mapToResponseDTO(facilitator);
    }

    @Transactional
    public void setCohortBatches(UUID facilitatorId, List<UUID> cohortBatchIds) {
        Facilitator facilitator = facilitatorRepository.findById(facilitatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found"));

        List<MeCohortBatch> batches = cohortBatchRepository.findAllById(cohortBatchIds);
        
        if (batches.size() != cohortBatchIds.size()) {
            throw new ResourceNotFoundException("One or more cohort batches not found");
        }

        facilitator.getCohortBatches().clear();
        facilitator.getCohortBatches().addAll(batches);
        facilitatorRepository.save(facilitator);
        
        // Auto-assign facilitator to courses in batch OR create if none exist
        for (MeCohortBatch batch : batches) {
            if (batch.getTracks().isEmpty()) {
                // No courses in batch - create one
                Course course = courseRepository.findAll().stream()
                    .filter(c -> c.getStatus() == com.dseme.app.enums.CourseStatus.ACTIVE)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No active course found"));
                
                MeCohort cohort = MeCohort.builder()
                    .name(batch.getName() + " - " + course.getName())
                    .batch(batch)
                    .course(course)
                    .startDate(batch.getStartDate())
                    .endDate(batch.getEndDate())
                    .maxParticipants(30)
                    .status(com.dseme.app.enums.CohortStatus.UPCOMING)
                    .build();
                
                cohortRepository.save(cohort);
            } else {
                // Courses exist - no auto-assignment needed (use junction table)
                // Facilitator assignment is handled through MeCohortFacilitator junction table
            }
        }
    }

    @Transactional
    public void setCohorts(UUID facilitatorId, List<UUID> cohortIds) {
        // This method is deprecated - use MeCohortFacilitatorRepository to manage facilitator-cohort relationships
        throw new UnsupportedOperationException("Use MeCohortFacilitatorRepository to manage facilitator-cohort relationships");
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

        List<CohortBatchSummaryDTO> assignedBatches = facilitator.getCohortBatches()
                .stream()
                .map(batch -> CohortBatchSummaryDTO.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .build())
                .collect(Collectors.toList());

        User user = facilitator.getUser();
        return FacilitatorResponseDTO.builder()
                .id(facilitator.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .employeeId(facilitator.getEmployeeId())
                .department(facilitator.getDepartment())
                .specialization(facilitator.getSpecialization())
                .status(user.getIsActive() ? "ACTIVE" : "INACTIVE")
                .partnerId(user.getPartner() != null ? user.getPartner().getPartnerId() : null)
                .partnerName(user.getPartner() != null ? user.getPartner().getPartnerName() : null)
                .centerId(user.getCenter() != null ? user.getCenter().getId() : null)
                .centerName(user.getCenter() != null ? user.getCenter().getCenterName() : null)
                .assignedCourses(assignedCourses)
                .assignedCohortBatches(assignedBatches)
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

    private User getCurrentUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}