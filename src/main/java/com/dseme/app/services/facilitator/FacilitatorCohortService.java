package com.dseme.app.services.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorCohortDTO;
import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.enums.ParticipantStatus;
import com.dseme.app.models.Facilitator;
import com.dseme.app.models.MeCohort;
import com.dseme.app.repositories.FacilitatorRepository;
import com.dseme.app.repositories.MeCohortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilitatorCohortService {

    private final MeCohortRepository cohortRepository;
    private final FacilitatorRepository facilitatorRepository;

    public List<FacilitatorCohortDTO> getFacilitatorCohorts(FacilitatorContext context) {
        Facilitator facilitator = facilitatorRepository.findByUserId(context.getFacilitator().getId())
                .orElseThrow(() -> new RuntimeException("Facilitator not found"));
        
        List<MeCohort> cohorts = cohortRepository.findByFacilitatorId(facilitator.getId());
        
        return cohorts.stream()
                .map(this::mapToDTO)
                .toList();
    }

    private FacilitatorCohortDTO mapToDTO(MeCohort cohort) {
        long totalParticipants = cohort.getParticipants() != null ? cohort.getParticipants().size() : 0;
        long activeParticipants = cohort.getParticipants() != null ? 
                cohort.getParticipants().stream()
                        .filter(p -> p.getStatus() == ParticipantStatus.ENROLLED || 
                                   p.getStatus() == ParticipantStatus.ACTIVE ||
                                   p.getStatus() == ParticipantStatus.IN_PROGRESS)
                        .count() : 0;

        return FacilitatorCohortDTO.builder()
                .cohortId(cohort.getId())
                .cohortName(cohort.getName())
                .courseName(cohort.getCourse() != null ? cohort.getCourse().getName() : null)
                .courseCode(cohort.getCourse() != null ? cohort.getCourse().getCode() : null)
                .batchName(cohort.getBatch() != null ? cohort.getBatch().getName() : null)
                .startDate(cohort.getStartDate())
                .endDate(cohort.getEndDate())
                .status(cohort.getStatus() != null ? cohort.getStatus().name() : null)
                .totalParticipants((int) totalParticipants)
                .activeParticipants((int) activeParticipants)
                .build();
    }
}
