package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.AnalyticsOverviewDTO;
import com.dseme.app.services.me.MeAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/analytics")
@RequiredArgsConstructor
@Tag(name = "ME Analytics & Reporting", description = "Endpoints for analytics and reporting in ME Portal")
public class MeAnalyticsController {

    private final MeAnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "System overview stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'ME_OFFICER', 'DONOR')")
    public ResponseEntity<AnalyticsOverviewDTO> getOverviewAnalytics() {
        AnalyticsOverviewDTO analytics = analyticsService.getOverviewAnalytics();
        return ResponseEntity.ok(analytics);
    }
}