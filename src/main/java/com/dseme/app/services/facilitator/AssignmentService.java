package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.AssignmentResponseDTO;
import com.dseme.app.dtos.facilitator.CreateAssignmentDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Assignment;
import com.dseme.app.models.Course;
import com.dseme.app.models.MeCohort;
import com.dseme.app.repositories.AssignmentRepository;
import com.dseme.app.repositories.CourseRepository;
import com.dseme.app.repositories.ScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final ScoreRepository scoreRepository;
    private final CohortIsolationService cohortIsolationService;

    public AssignmentResponseDTO createAssignment(FacilitatorContext context, CreateAssignmentDTO dto) {
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);

        if (activeCohort.getStatus() != CohortStatus.ACTIVE) {
            throw new AccessDeniedException("Cannot create assignment for non-active cohort");
        }

        Course course = activeCohort.getCourse();
        if (course == null) {
            throw new ResourceNotFoundException("Cohort has no course assigned");
        }

        Assignment assignment = Assignment.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .course(course)
                .cohort(activeCohort)
                .dueDate(dto.getDueDate())
                .maxScore(dto.getMaxScore())
                .createdBy(context.getFacilitator())
                .build();

        assignment = assignmentRepository.save(assignment);
        return mapToDTO(assignment, activeCohort);
    }

    public List<AssignmentResponseDTO> getCohortAssignments(FacilitatorContext context) {
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        List<Assignment> assignments = assignmentRepository.findByCohortId(activeCohort.getId());
        return assignments.stream()
                .map(a -> mapToDTO(a, activeCohort))
                .collect(Collectors.toList());
    }

    public AssignmentResponseDTO getAssignment(FacilitatorContext context, UUID assignmentId) {
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (!assignment.getCohort().getId().equals(activeCohort.getId())) {
            throw new AccessDeniedException("Assignment does not belong to your cohort");
        }

        return mapToDTO(assignment, activeCohort);
    }

    public AssignmentResponseDTO updateAssignment(FacilitatorContext context, UUID assignmentId, CreateAssignmentDTO dto) {
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (!assignment.getCohort().getId().equals(activeCohort.getId())) {
            throw new AccessDeniedException("Assignment does not belong to your cohort");
        }

        Course course = activeCohort.getCourse();
        if (course == null) {
            throw new ResourceNotFoundException("Cohort has no course assigned");
        }

        assignment.setTitle(dto.getTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setType(dto.getType());
        assignment.setCourse(course);
        assignment.setDueDate(dto.getDueDate());
        assignment.setMaxScore(dto.getMaxScore());

        assignment = assignmentRepository.save(assignment);
        return mapToDTO(assignment, activeCohort);
    }

    public void deleteAssignment(FacilitatorContext context, UUID assignmentId) {
        MeCohort activeCohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        if (!assignment.getCohort().getId().equals(activeCohort.getId())) {
            throw new AccessDeniedException("Assignment does not belong to your cohort");
        }

        assignmentRepository.delete(assignment);
    }

    private AssignmentResponseDTO mapToDTO(Assignment assignment, MeCohort cohort) {
        int totalStudents = cohort.getParticipants().size();
        int gradedStudents = assignment.getCourse() != null ? 
            scoreRepository.findByCourseId(assignment.getCourse().getId()).size() : 0;

        return AssignmentResponseDTO.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .type(assignment.getType())
                .moduleId(assignment.getCourse() != null ? assignment.getCourse().getId() : null)
                .moduleName(assignment.getCourse() != null ? assignment.getCourse().getName() : "N/A")
                .course(assignment.getCourse() != null ? assignment.getCourse().getName() : "N/A")
                .chapter(assignment.getTitle())
                .dueDate(assignment.getDueDate())
                .maxScore(assignment.getMaxScore())
                .totalStudents(totalStudents)
                .gradedStudents(gradedStudents)
                .createdByName(assignment.getCreatedBy().getFirstName() + " " + assignment.getCreatedBy().getLastName())
                .createdAt(assignment.getCreatedAt())
                .build();
    }
}
