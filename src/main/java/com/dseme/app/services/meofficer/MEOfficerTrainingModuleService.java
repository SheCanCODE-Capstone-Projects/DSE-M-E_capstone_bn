package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.MEOfficerContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER training module management.
 * 
 * ME_OFFICER can:
 * - Create training modules for their partner's programs
 * - Edit training modules (only for active cohorts)
 * - Delete training modules (only if no assignments exist)
 * - Assign modules to facilitators (only for active cohorts)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MEOfficerTrainingModuleService {

    private final TrainingModuleRepository trainingModuleRepository;
    private final ProgramRepository programRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final ModuleAssignmentRepository moduleAssignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ScoreRepository scoreRepository;

    /**
     * Creates a training module for a program.
     * ME_OFFICER can create modules for any program in their partner.
     */
    public TrainingModule createTrainingModule(
            MEOfficerContext context,
            com.dseme.app.dtos.meofficer.CreateTrainingModuleRequestDTO dto
    ) {
        // Load program
        Program program = programRepository.findById(dto.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Program not found with ID: " + dto.getProgramId()
                ));

        // Validate program belongs to ME_OFFICER's partner
        if (!program.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Program does not belong to your assigned partner."
            );
        }

        // Create training module
        TrainingModule module = TrainingModule.builder()
                .program(program)
                .moduleName(dto.getModuleName())
                .description(dto.getDescription())
                .sequenceOrder(dto.getSequenceOrder())
                .durationHours(dto.getDurationHours())
                .isMandatory(dto.getIsMandatory() != null ? dto.getIsMandatory() : false)
                .createdBy(context.getMeOfficer())
                .build();

        return trainingModuleRepository.save(module);
    }

    /**
     * Updates a training module.
     * Only allowed if module belongs to an active cohort.
     */
    public TrainingModule updateTrainingModule(
            MEOfficerContext context,
            UUID moduleId,
            com.dseme.app.dtos.meofficer.UpdateTrainingModuleRequestDTO dto
    ) {
        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to ME_OFFICER's partner
        if (!module.getProgram().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your assigned partner."
            );
        }

        // Check if module belongs to an active cohort
        List<Cohort> cohorts = cohortRepository.findByCenterPartnerPartnerId(context.getPartnerId());
        boolean belongsToActiveCohort = cohorts.stream()
                .filter(c -> c.getStatus() == CohortStatus.ACTIVE)
                .anyMatch(c -> c.getProgram().getId().equals(module.getProgram().getId()));

        if (!belongsToActiveCohort) {
            throw new AccessDeniedException(
                "Access denied. Cannot edit modules for cohorts that have ended. " +
                "Only modules in active cohorts can be edited."
            );
        }

        // Update fields
        if (dto.getModuleName() != null) {
            module.setModuleName(dto.getModuleName());
        }
        if (dto.getDescription() != null) {
            module.setDescription(dto.getDescription());
        }
        if (dto.getSequenceOrder() != null) {
            module.setSequenceOrder(dto.getSequenceOrder());
        }
        if (dto.getDurationHours() != null) {
            module.setDurationHours(dto.getDurationHours());
        }
        if (dto.getIsMandatory() != null) {
            module.setIsMandatory(dto.getIsMandatory());
        }

        return trainingModuleRepository.save(module);
    }

    /**
     * Deletes a training module.
     * Only allowed if module has no assignments, enrollments, attendance, or scores.
     */
    public void deleteTrainingModule(MEOfficerContext context, UUID moduleId) {
        // Load module
        TrainingModule module = trainingModuleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + moduleId
                ));

        // Validate module belongs to ME_OFFICER's partner
        if (!module.getProgram().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your assigned partner."
            );
        }

        // Check if module has assignments
        List<ModuleAssignment> assignments = moduleAssignmentRepository.findByModuleId(moduleId);
        if (!assignments.isEmpty()) {
            throw new AccessDeniedException(
                "Cannot delete module. Module has " + assignments.size() + " active assignments. " +
                "Please unassign all facilitators before deleting."
            );
        }

        // Check if module has enrollments
        List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                .filter(e -> e.getModule() != null && e.getModule().getId().equals(moduleId))
                .collect(Collectors.toList());
        if (!enrollments.isEmpty()) {
            throw new AccessDeniedException(
                "Cannot delete module. Module has " + enrollments.size() + " enrollments. " +
                "Cannot delete modules with existing enrollments."
            );
        }

        // Check if module has attendance records
        long attendanceCount = attendanceRepository.findAll().stream()
                .filter(a -> a.getModule().getId().equals(moduleId))
                .count();
        if (attendanceCount > 0) {
            throw new AccessDeniedException(
                "Cannot delete module. Module has " + attendanceCount + " attendance records. " +
                "Cannot delete modules with attendance data."
            );
        }

        // Check if module has score records
        long scoreCount = scoreRepository.findAll().stream()
                .filter(s -> s.getModule().getId().equals(moduleId))
                .count();
        if (scoreCount > 0) {
            throw new AccessDeniedException(
                "Cannot delete module. Module has " + scoreCount + " score records. " +
                "Cannot delete modules with score data."
            );
        }

        trainingModuleRepository.delete(module);
    }

    /**
     * Assigns a module to a facilitator.
     * Only allowed if:
     * - Facilitator belongs to same partner
     * - Cohort is active
     * - Module belongs to cohort's program
     */
    public ModuleAssignment assignModuleToFacilitator(
            MEOfficerContext context,
            com.dseme.app.dtos.meofficer.AssignModuleRequestDTO dto
    ) {
        // Load facilitator
        User facilitator = userRepository.findById(dto.getFacilitatorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Facilitator not found with ID: " + dto.getFacilitatorId()
                ));

        // Validate facilitator has FACILITATOR role
        if (facilitator.getRole() != Role.FACILITATOR) {
            throw new AccessDeniedException(
                "Access denied. User is not a FACILITATOR. Role: " + facilitator.getRole()
            );
        }

        // Validate facilitator belongs to same partner
        if (facilitator.getPartner() == null || 
            !facilitator.getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Facilitator does not belong to your assigned partner."
            );
        }

        // Load module
        TrainingModule module = trainingModuleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Training module not found with ID: " + dto.getModuleId()
                ));

        // Validate module belongs to ME_OFFICER's partner
        if (!module.getProgram().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to your assigned partner."
            );
        }

        // Load cohort
        Cohort cohort = cohortRepository.findById(dto.getCohortId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Cohort not found with ID: " + dto.getCohortId()
                ));

        // Validate cohort belongs to ME_OFFICER's partner
        if (!cohort.getCenter().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Cohort does not belong to your assigned partner."
            );
        }

        // Validate cohort is ACTIVE
        if (cohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException(
                "Access denied. Cannot assign modules to cohorts that have ended. " +
                "Cohort status: " + cohort.getStatus() + ". Only ACTIVE cohorts allow module assignments."
            );
        }

        // Validate module belongs to cohort's program
        if (!module.getProgram().getId().equals(cohort.getProgram().getId())) {
            throw new AccessDeniedException(
                "Access denied. Module does not belong to cohort's program. " +
                "Module program: " + module.getProgram().getProgramName() + ", " +
                "Cohort program: " + cohort.getProgram().getProgramName()
            );
        }

        // Check if assignment already exists
        if (moduleAssignmentRepository.existsByFacilitatorIdAndModuleIdAndCohortId(
                facilitator.getId(), module.getId(), cohort.getId())) {
            throw new ResourceAlreadyExistsException(
                "Module is already assigned to this facilitator for this cohort."
            );
        }

        // Create assignment
        ModuleAssignment assignment = ModuleAssignment.builder()
                .facilitator(facilitator)
                .module(module)
                .cohort(cohort)
                .assignedBy(context.getMeOfficer())
                .build();

        return moduleAssignmentRepository.save(assignment);
    }

    /**
     * Gets all modules for ME_OFFICER's partner.
     */
    @Transactional(readOnly = true)
    public List<TrainingModule> getAllModules(MEOfficerContext context) {
        List<Program> programs = programRepository.findByPartnerPartnerId(context.getPartnerId());
        
        return programs.stream()
                .flatMap(program -> trainingModuleRepository.findByProgramId(program.getId()).stream())
                .collect(Collectors.toList());
    }

    /**
     * Gets all module assignments for ME_OFFICER's partner.
     */
    @Transactional(readOnly = true)
    public List<com.dseme.app.dtos.meofficer.ModuleAssignmentResponseDTO> getAllModuleAssignments(
            MEOfficerContext context) {
        List<ModuleAssignment> assignments = moduleAssignmentRepository
                .findByFacilitatorPartnerPartnerId(context.getPartnerId());
        
        return assignments.stream()
                .map(this::mapToAssignmentResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Unassigns a module from a facilitator.
     */
    public void unassignModule(
            MEOfficerContext context,
            UUID facilitatorId,
            UUID moduleId,
            UUID cohortId
    ) {
        ModuleAssignment assignment = moduleAssignmentRepository
                .findByFacilitatorIdAndModuleIdAndCohortId(facilitatorId, moduleId, cohortId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Module assignment not found"
                ));

        // Validate assignment belongs to ME_OFFICER's partner
        if (!assignment.getFacilitator().getPartner().getPartnerId().equals(context.getPartnerId())) {
            throw new AccessDeniedException(
                "Access denied. Assignment does not belong to your assigned partner."
            );
        }

        moduleAssignmentRepository.delete(assignment);
    }

    private com.dseme.app.dtos.meofficer.ModuleAssignmentResponseDTO mapToAssignmentResponseDTO(
            ModuleAssignment assignment) {
        return com.dseme.app.dtos.meofficer.ModuleAssignmentResponseDTO.builder()
                .assignmentId(assignment.getId())
                .facilitatorId(assignment.getFacilitator().getId())
                .facilitatorName(assignment.getFacilitator().getFirstName() + " " + 
                        assignment.getFacilitator().getLastName())
                .facilitatorEmail(assignment.getFacilitator().getEmail())
                .moduleId(assignment.getModule().getId())
                .moduleName(assignment.getModule().getModuleName())
                .cohortId(assignment.getCohort().getId())
                .cohortName(assignment.getCohort().getCohortName())
                .assignedById(assignment.getAssignedBy().getId())
                .assignedByName(assignment.getAssignedBy().getFirstName() + " " + 
                        assignment.getAssignedBy().getLastName())
                .assignedAt(assignment.getAssignedAt())
                .build();
    }
}
