package com.dseme.app.services.me;

import com.dseme.app.dtos.me.AssignFacilitatorsDTO;
import com.dseme.app.dtos.me.FacilitatorSummaryDTO;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Facilitator;
import com.dseme.app.models.MeCohort;
import com.dseme.app.models.MeCohortFacilitator;
import com.dseme.app.repositories.FacilitatorRepository;
import com.dseme.app.repositories.MeCohortFacilitatorRepository;
import com.dseme.app.repositories.MeCohortRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeCohortFacilitatorService {

    private final MeCohortFacilitatorRepository cohortFacilitatorRepository;
    private final MeCohortRepository cohortRepository;
    private final FacilitatorRepository facilitatorRepository;

    @Transactional
    public List<FacilitatorSummaryDTO> assignFacilitators(UUID cohortId, AssignFacilitatorsDTO dto) {
        MeCohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort not found"));

        // Remove existing assignments
        cohortFacilitatorRepository.findByCohortId(cohortId)
                .forEach(cohortFacilitatorRepository::delete);

        // Add new assignments
        List<MeCohortFacilitator> assignments = dto.getFacilitatorIds().stream()
                .map(facilitatorId -> {
                    Facilitator facilitator = facilitatorRepository.findById(facilitatorId)
                            .orElseThrow(() -> new ResourceNotFoundException("Facilitator not found: " + facilitatorId));

                    return MeCohortFacilitator.builder()
                            .cohortId(cohortId)
                            .facilitatorId(facilitatorId)
                            .role("FACILITATOR")
                            .build();
                })
                .collect(Collectors.toList());

        cohortFacilitatorRepository.saveAll(assignments);

        return getFacilitatorsByCohort(cohortId);
    }

    public List<FacilitatorSummaryDTO> getFacilitatorsByCohort(UUID cohortId) {
        return cohortFacilitatorRepository.findByCohortId(cohortId).stream()
                .map(cf -> {
                    Facilitator fac = facilitatorRepository.findById(cf.getFacilitatorId())
                            .orElse(null);
                    if (fac == null || fac.getUser() == null) {
                        return null;
                    }
                    return FacilitatorSummaryDTO.builder()
                            .id(fac.getId())
                            .firstName(fac.getUser().getFirstName())
                            .lastName(fac.getUser().getLastName())
                            .build();
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public List<UUID> getCohortIdsByFacilitator(UUID facilitatorId) {
        return cohortFacilitatorRepository.findCohortIdsByFacilitatorId(facilitatorId);
    }

    @Transactional
    public void removeFacilitator(UUID cohortId, UUID facilitatorId) {
        cohortFacilitatorRepository.deleteByCohortIdAndFacilitatorId(cohortId, facilitatorId);
    }
}
