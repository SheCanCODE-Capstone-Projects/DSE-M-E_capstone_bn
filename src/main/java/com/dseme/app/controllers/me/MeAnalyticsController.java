package com.dseme.app.controllers.me;

import com.dseme.app.dtos.me.AnalyticsOverviewDTO;
import com.dseme.app.services.me.MeAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me/analytics")
@RequiredArgsConstructor
@Tag(name = "ME Analytics & Reporting", description = "Endpoints for analytics and reporting in ME Portal")
public class MeAnalyticsController {

    private final MeAnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "System overview stats")
    public ResponseEntity<AnalyticsOverviewDTO> getOverviewAnalytics() {
        AnalyticsOverviewDTO analytics = analyticsService.getOverviewAnalytics();
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/retention-trend")
    @Operation(summary = "Get retention trend data for charts")
    public ResponseEntity<List<Map<String, Object>>> getRetentionTrend() {
        return ResponseEntity.ok(analyticsService.getRetentionTrend());
    }

    @GetMapping("/attendance-summary")
    @Operation(summary = "Get attendance summary")
    public ResponseEntity<Map<String, Object>> getAttendanceSummary() {
        return ResponseEntity.ok(analyticsService.getAttendanceSummary());
    }

    @GetMapping("/top-performers")
    @Operation(summary = "Get top performing participants")
    public ResponseEntity<List<Map<String, Object>>> getTopPerformers() {
        return ResponseEntity.ok(analyticsService.getTopPerformers());
    }
}