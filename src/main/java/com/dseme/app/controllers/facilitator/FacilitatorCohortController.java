package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorCohortDTO;
import com.dseme.app.services.facilitator.FacilitatorCohortService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facilitator/my-cohorts")
@RequiredArgsConstructor
public class FacilitatorCohortController extends FacilitatorBaseController {

    private final FacilitatorCohortService cohortService;

    @GetMapping
    public ResponseEntity<List<FacilitatorCohortDTO>> getMyCohorts(HttpServletRequest request) {
        return ResponseEntity.ok(cohortService.getFacilitatorCohorts(getFacilitatorContext(request)));
    }
}
