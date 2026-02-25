package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.enums.AssessmentType;
import com.dseme.app.models.*;
import com.dseme.app.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentGradingService {

    private final AssignmentRepository assignmentRepository;
    private final ScoreRepository scoreRepository;
    private final MeParticipantRepository participantRepository;
    private final MeCohortRepository cohortRepository;
    private final TrainingModuleRepository moduleRepository;
    private final CohortIsolationService cohortIsolationService;

    // ==================== CREATE ASSIGNMENT ====================
    public AssignmentResponseDTO createAssignment(FacilitatorContext context, CreateAssignmentDTO dto) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        TrainingModule module = moduleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new RuntimeException("Module not found"));
        
        Assignment assignment = Assignment.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .type(dto.getType())
                .module(module)
                .cohort(cohort)
                .dueDate(dto.getDueDate())
                .maxScore(dto.getMaxScore())
                .createdBy(context.getFacilitator())
                .build();
        
        assignment = assignmentRepository.save(assignment);
        
        return toResponseDTO(assignment, cohort);
    }

    // ==================== GET ALL ASSIGNMENTS FOR COHORT ====================
    @Transactional(readOnly = true)
    public List<AssignmentResponseDTO> getAssignmentsByCohort(FacilitatorContext context) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        List<Assignment> assignments = assignmentRepository.findByCohortId(cohort.getId());
        
        return assignments.stream()
                .map(a -> toResponseDTO(a, cohort))
                .collect(Collectors.toList());
    }

    // ==================== GET SINGLE ASSIGNMENT WITH GRADES ====================
    @Transactional(readOnly = true)
    public AssignmentWithGradesDTO getAssignmentWithGrades(FacilitatorContext context, UUID assignmentId) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Assignment does not belong to your cohort");
        }
        
        List<MeParticipant> participants = participantRepository.findByCohortId(cohort.getId());
        List<Score> scores = assignment.getScores();
        
        List<ParticipantGradeDTO> grades = participants.stream()
                .map(participant -> {
                    Score score = scores.stream()
                            .filter(s -> s.getParticipant().getId().equals(participant.getId()))
                            .findFirst()
                            .orElse(null);
                    
                    if (score != null) {
                        BigDecimal percentage = score.getScoreValue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(assignment.getMaxScore()), 2, RoundingMode.HALF_UP);
                        
                        return ParticipantGradeDTO.builder()
                                .participantId(participant.getId())
                                .participantName(participant.getUser().getFirstName() + " " + participant.getUser().getLastName())
                                .participantEmail(participant.getUser().getEmail())
                                .score(score.getScoreValue())
                                .maxScore(assignment.getMaxScore())
                                .percentage(percentage)
                                .recordedByName(score.getRecordedBy().getFirstName() + " " + score.getRecordedBy().getLastName())
                                .recordedAt(score.getRecordedAt())
                                .build();
                    } else {
                        return ParticipantGradeDTO.builder()
                                .participantId(participant.getId())
                                .participantName(participant.getUser().getFirstName() + " " + participant.getUser().getLastName())
                                .participantEmail(participant.getUser().getEmail())
                                .score(null)
                                .maxScore(assignment.getMaxScore())
                                .percentage(null)
                                .recordedByName(null)
                                .recordedAt(null)
                                .build();
                    }
                })
                .collect(Collectors.toList());
        
        long gradedCount = grades.stream().filter(g -> g.getScore() != null).count();
        
        return AssignmentWithGradesDTO.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .type(assignment.getType())
                .course(assignment.getModule().getProgram().getProgramName())
                .chapter(assignment.getModule().getModuleName())
                .dueDate(assignment.getDueDate())
                .maxScore(assignment.getMaxScore())
                .totalStudents(participants.size())
                .gradedStudents((int) gradedCount)
                .grades(grades)
                .createdByName(assignment.getCreatedBy().getFirstName() + " " + assignment.getCreatedBy().getLastName())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    // ==================== BATCH GRADE PARTICIPANTS ====================
    public List<ParticipantGradeDTO> batchGradeParticipants(FacilitatorContext context, BatchGradeRequest request) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Assignment does not belong to your cohort");
        }
        
        List<ParticipantGradeDTO> results = new ArrayList<>();
        
        for (GradeParticipantDTO gradeDTO : request.getGrades()) {
            MeParticipant participant = participantRepository.findById(gradeDTO.getParticipantId())
                    .orElseThrow(() -> new RuntimeException("Participant not found: " + gradeDTO.getParticipantId()));
            
            if (!participant.getCohort().getId().equals(cohort.getId())) {
                throw new RuntimeException("Participant does not belong to your cohort");
            }
            
            // Check if score already exists
            Score existingScore = assignment.getScores().stream()
                    .filter(s -> s.getParticipant().getId().equals(participant.getId()))
                    .findFirst()
                    .orElse(null);
            
            Score score;
            if (existingScore != null) {
                existingScore.setScoreValue(gradeDTO.getScore());
                existingScore.setRecordedBy(context.getFacilitator());
                existingScore.setRecordedAt(Instant.now());
                score = scoreRepository.save(existingScore);
            } else {
                score = Score.builder()
                        .participant(participant)
                        .module(assignment.getModule())
                        .assignment(assignment)
                        .assessmentType(assignment.getType())
                        .assessmentName(assignment.getTitle())
                        .scoreValue(gradeDTO.getScore())
                        .recordedBy(context.getFacilitator())
                        .recordedAt(Instant.now())
                        .build();
                score = scoreRepository.save(score);
            }
            
            BigDecimal percentage = score.getScoreValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(assignment.getMaxScore()), 2, RoundingMode.HALF_UP);
            
            results.add(ParticipantGradeDTO.builder()
                    .participantId(participant.getId())
                    .participantName(participant.getUser().getFirstName() + " " + participant.getUser().getLastName())
                    .participantEmail(participant.getUser().getEmail())
                    .score(score.getScoreValue())
                    .maxScore(assignment.getMaxScore())
                    .percentage(percentage)
                    .recordedByName(context.getFacilitator().getFirstName() + " " + context.getFacilitator().getLastName())
                    .recordedAt(score.getRecordedAt())
                    .build());
        }
        
        return results;
    }

    // ==================== UPDATE ASSIGNMENT ====================
    public AssignmentResponseDTO updateAssignment(FacilitatorContext context, UUID assignmentId, CreateAssignmentDTO dto) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Assignment does not belong to your cohort");
        }
        
        TrainingModule module = moduleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new RuntimeException("Module not found"));
        
        assignment.setTitle(dto.getTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setType(dto.getType());
        assignment.setModule(module);
        assignment.setDueDate(dto.getDueDate());
        assignment.setMaxScore(dto.getMaxScore());
        
        assignment = assignmentRepository.save(assignment);
        
        return toResponseDTO(assignment, cohort);
    }

    // ==================== DELETE ASSIGNMENT ====================
    public void deleteAssignment(FacilitatorContext context, UUID assignmentId) {
        MeCohort cohort = cohortIsolationService.getFacilitatorActiveCohort(context);
        
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
        
        if (!assignment.getCohort().getId().equals(cohort.getId())) {
            throw new RuntimeException("Assignment does not belong to your cohort");
        }
        
        assignmentRepository.delete(assignment);
    }

    // ==================== HELPER METHODS ====================
    private AssignmentResponseDTO toResponseDTO(Assignment assignment, MeCohort cohort) {
        List<MeParticipant> participants = participantRepository.findByCohortId(cohort.getId());
        long gradedCount = assignment.getScores().size();
        
        return AssignmentResponseDTO.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .type(assignment.getType())
                .moduleId(assignment.getModule().getId())
                .moduleName(assignment.getModule().getModuleName())
                .course(assignment.getModule().getProgram().getProgramName())
                .chapter(assignment.getModule().getModuleName())
                .dueDate(assignment.getDueDate())
                .maxScore(assignment.getMaxScore())
                .totalStudents(participants.size())
                .gradedStudents((int) gradedCount)
                .createdByName(assignment.getCreatedBy().getFirstName() + " " + assignment.getCreatedBy().getLastName())
                .createdAt(assignment.getCreatedAt())
                .build();
    }
}
