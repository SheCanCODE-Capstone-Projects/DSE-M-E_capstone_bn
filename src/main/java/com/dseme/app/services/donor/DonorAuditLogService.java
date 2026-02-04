package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.AuditLogFilterDTO;
import com.dseme.app.dtos.donor.AuditLogListResponseDTO;
import com.dseme.app.dtos.donor.AuditLogResponseDTO;
import com.dseme.app.dtos.donor.DonorContext;
import com.dseme.app.models.AuditLog;
import com.dseme.app.models.Partner;
import com.dseme.app.repositories.AuditLogRepository;
import com.dseme.app.repositories.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service for DONOR audit log visibility.
 * 
 * Provides read-only access to audit logs across all partners.
 * Supports filtering by action type, date range, and partner.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonorAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final PartnerRepository partnerRepository;

    /**
     * Gets audit logs with optional filters.
     * 
     * Supports filtering by:
     * - Action type
     * - Entity type
     * - Date range
     * - Actor role
     * - Partner (optional)
     * 
     * @param context DONOR context
     * @param filter Filter criteria
     * @return Paginated audit log list
     */
    public AuditLogListResponseDTO getAuditLogs(DonorContext context, AuditLogFilterDTO filter) {
        // Build pageable
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(filter.getSortDirection()) ? 
                        Sort.Direction.DESC : Sort.Direction.ASC,
                filter.getSortBy()
        );
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        // Query audit logs with filters
        Page<AuditLog> auditLogPage = auditLogRepository.findWithFilters(
                filter.getAction(),
                filter.getEntityType(),
                filter.getActorRole(),
                filter.getDateRangeStart(),
                filter.getDateRangeEnd(),
                pageable
        );

        // Convert to DTOs
        List<AuditLogResponseDTO> auditLogDTOs = auditLogPage.getContent().stream()
                .map(this::mapToAuditLogResponseDTO)
                .collect(Collectors.toList());

        // Apply partner filter if specified
        if (filter.getPartnerId() != null && !filter.getPartnerId().trim().isEmpty()) {
            auditLogDTOs = auditLogDTOs.stream()
                    .filter(log -> filter.getPartnerId().equals(log.getPartnerId()))
                    .collect(Collectors.toList());
        }

        return AuditLogListResponseDTO.builder()
                .auditLogs(auditLogDTOs)
                .currentPage(auditLogPage.getNumber())
                .pageSize(auditLogPage.getSize())
                .totalPages(auditLogPage.getTotalPages())
                .totalElements(auditLogPage.getTotalElements())
                .isFirst(auditLogPage.isFirst())
                .isLast(auditLogPage.isLast())
                .build();
    }

    /**
     * Maps AuditLog entity to AuditLogResponseDTO.
     * Extracts partner information from entity relationships where applicable.
     */
    private AuditLogResponseDTO mapToAuditLogResponseDTO(AuditLog auditLog) {
        // Extract actor information
        String actorEmail = auditLog.getActor() != null ? auditLog.getActor().getEmail() : "Unknown";
        String actorName = "";
        if (auditLog.getActor() != null) {
            String firstName = auditLog.getActor().getFirstName();
            String lastName = auditLog.getActor().getLastName();
            actorName = (firstName != null ? firstName : "") +
                       (lastName != null ? " " + lastName : "");
            actorName = actorName.trim();
            if (actorName.isEmpty()) {
                actorName = actorEmail;
            }
        }

        // Try to extract partner information from entity relationships
        String partnerId = null;
        AtomicReference<String> partnerName = new AtomicReference<>();

        // Check if entity is related to a partner
        if (auditLog.getEntityType() != null && auditLog.getEntityId() != null) {
            partnerId = extractPartnerIdFromEntity(auditLog.getEntityType(), auditLog.getEntityId());
            if (partnerId != null) {
                partnerRepository.findById(partnerId).ifPresent(partner -> {
                    partnerName.set(partner.getPartnerName());
                });
            }
        }

        // Also check if actor is associated with a partner
        if (partnerId == null && auditLog.getActor() != null && auditLog.getActor().getPartner() != null) {
            Partner actorPartner = auditLog.getActor().getPartner();
            partnerId = actorPartner.getPartnerId();
            partnerName.set(actorPartner.getPartnerName());
        }

        // Final variables for lambda
        final String finalPartnerId = partnerId;
        final String finalPartnerName = partnerName.get();

        return AuditLogResponseDTO.builder()
                .auditLogId(auditLog.getId())
                .actorId(auditLog.getActor() != null ? auditLog.getActor().getId() : null)
                .actorEmail(actorEmail)
                .actorName(actorName)
                .actorRole(auditLog.getActorRole())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .createdAt(auditLog.getCreatedAt())
                .partnerId(finalPartnerId)
                .partnerName(finalPartnerName)
                .build();
    }

    /**
     * Attempts to extract partner ID from entity based on entity type.
     * This is a helper method that may need to query different repositories
     * based on entity type. For now, we'll return null and let the actor's
     * partner be used as fallback.
     */
    private String extractPartnerIdFromEntity(String entityType, java.util.UUID entityId) {
        // This is a simplified implementation
        // In a full implementation, you might query different repositories
        // based on entityType (e.g., Participant, Enrollment, etc.)
        // For now, we'll rely on the actor's partner as the primary source
        
        // TODO: Implement entity-specific partner extraction if needed
        // Examples:
        // - If entityType is "PARTICIPANT", query ParticipantRepository
        // - If entityType is "ENROLLMENT", query EnrollmentRepository -> Participant -> Partner
        // - If entityType is "PARTNER", the entityId might be the partnerId itself
        
        return null;
    }
}
