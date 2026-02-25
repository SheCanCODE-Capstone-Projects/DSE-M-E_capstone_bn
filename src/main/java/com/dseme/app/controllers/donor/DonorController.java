package com.dseme.app.controllers.donor;

import com.dseme.app.dtos.donor.DonorStatisticsDTO;
import com.dseme.app.services.donor.DonorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DONOR')")
public class DonorController {
    
    private final DonorService donorService;
    
    @GetMapping("/statistics")
    public ResponseEntity<DonorStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(donorService.getStatistics());
    }
}
