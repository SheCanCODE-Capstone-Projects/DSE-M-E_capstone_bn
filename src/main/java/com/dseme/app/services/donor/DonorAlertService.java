package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.AlertListResponseDTO;
import com.dseme.app.dtos.donor.AlertSummaryDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.enums.AlertSeverity;
import com.dseme.app.models.Alert;
import com.dseme.app.repositories.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for DONOR alert management.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DonorAlertService {

    private final AlertRepository alertRepository;

    /**
     * Gets all alerts with pagination and filtering.
     */
    @Transactional(readOnly = true)
    public AlertListResponseDTO getAlerts(
            int page,
            int size,
            String partnerId,
            AlertSeverity severity,
            Boolean isResolved
    ) {
        List<Alert> allAlerts = alertRepository.findAll();
        
        // Apply filters
        List<Alert> filtered = allAlerts.stream()
                .filter(a -> partnerId == null || a.getPartner().getPartnerId().equals(partnerId))
                .filter(a -> severity == null || a.getSeverity() == severity)
                .filter(a -> isResolved == null || Boolean.TRUE.equals(a.getIsResolved()) == isResolved)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<Alert> paged = start < filtered.size() ? filtered.subList(start, end) : List.of();

        List<AlertSummaryDTO> summaries = paged.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());

        long unresolvedCount = filtered.stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsResolved()))
                .count();

        int totalPages = (int) Math.ceil((double) filtered.size() / size);

        return AlertListResponseDTO.builder()
                .alerts(summaries)
                .totalCount((long) filtered.size())
                .unresolvedCount(unresolvedCount)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .build();
    }

    /**
     * Gets alert details by ID.
     */
    @Transactional(readOnly = true)
    public AlertSummaryDTO getAlertById(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                    "Alert with ID '" + alertId + "' not found."
                ));

        return mapToSummaryDTO(alert);
    }

    /**
     * Resolves an alert.
     */
    public void resolveAlert(DonorContext context, UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new com.dseme.app.exceptions.ResourceNotFoundException(
                    "Alert with ID '" + alertId + "' not found."
                ));

        alert.setIsResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setResolvedBy(context.getUser());
        alertRepository.save(alert);
    }

    /**
     * Maps Alert entity to AlertSummaryDTO.
     */
    private AlertSummaryDTO mapToSummaryDTO(Alert alert) {
        return AlertSummaryDTO.builder()
                .id(alert.getId())
                .partnerId(alert.getPartner().getPartnerId())
                .partnerName(alert.getPartner().getPartnerName())
                .severity(alert.getSeverity())
                .alertType(alert.getAlertType())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .issueCount(alert.getIssueCount())
                .callToAction(alert.getCallToAction())
                .relatedEntityType(alert.getRelatedEntityType())
                .relatedEntityId(alert.getRelatedEntityId())
                .isResolved(alert.getIsResolved())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy() != null ? 
                        alert.getResolvedBy().getEmail() : null)
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
