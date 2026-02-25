package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.repositories.FacilitatorRepository;
import com.dseme.app.services.facilitator.CohortIsolationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for facilitators to get available cohorts for participant enrollment.
 */
@RestController
@RequestMapping("/api/facilitator/cohorts")
@RequiredArgsConstructor
public class CohortController extends FacilitatorBaseController {

    private final CohortIsolationService cohortIsolationService;
    private final FacilitatorRepository facilitatorRepository;

    /**
     * Gets available cohorts for the facilitator to enroll participants.
     * Returns:
     * 1. Active cohorts in facilitator's center (from Cohort table)
     * 2. Cohorts from facilitator's assigned cohort batches (MeCohort)
     * 
     * GET /api/facilitator/cohorts
     * 
     * @param request HTTP request (contains FacilitatorContext)
     * @return List of available cohorts
     */
    @GetMapping
    public ResponseEntity<List<CohortOptionDTO>> getAvailableCohorts(HttpServletRequest request) {
        FacilitatorContext context = getFacilitatorContext(request);
        List<CohortOptionDTO> options = new ArrayList<>();
        
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CohortController.class);
        log.info("Getting cohorts for facilitator: {}", context.getFacilitator().getEmail());
        log.info("Facilitator center: {}", context.getCenterId());
        
        // Option 1: Get active cohorts from facilitator's center
        List<com.dseme.app.models.MeCohort> centerCohorts = cohortIsolationService.getActiveCohortsForFacilitator(context);
        log.info("Found {} active cohorts in center", centerCohorts.size());
        
        options.addAll(centerCohorts.stream()
                .map(cohort -> {
                    log.info("Center cohort: {} ({})", cohort.getName(), cohort.getId());
                    return CohortOptionDTO.builder()
                            .id(cohort.getId())
                            .name(cohort.getName())
                            .status(cohort.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList()));
        
        // Option 2: Get cohorts from facilitator's assigned cohort batches
        com.dseme.app.models.Facilitator facilitator = facilitatorRepository.findByUserId(context.getFacilitator().getId())
                .orElse(null);
        
        if (facilitator != null && facilitator.getCohortBatches() != null) {
            log.info("Facilitator has {} assigned cohort batches", facilitator.getCohortBatches().size());
            facilitator.getCohortBatches().forEach(batch -> {
                log.info("Batch: {} ({})", batch.getName(), batch.getId());
                if (batch.getTracks() != null) {
                    log.info("Batch has {} tracks", batch.getTracks().size());
                    batch.getTracks().forEach(track -> {
                        log.info("Track: {} ({})", track.getName(), track.getId());
                        options.add(CohortOptionDTO.builder()
                                .id(track.getId())
                                .name(track.getName() + " (" + batch.getName() + ")")
                                .status(batch.getStatus().name())
                                .build());
                    });
                }
            });
        } else {
            log.info("Facilitator has no assigned cohort batches");
        }
        
        log.info("Returning {} total cohort options", options.size());
        return ResponseEntity.ok(options);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CohortOptionDTO {
        private UUID id;
        private String name;
        private String status;
    }
}
