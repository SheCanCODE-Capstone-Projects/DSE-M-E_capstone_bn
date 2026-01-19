package com.dseme.app.services.meofficer;

import com.dseme.app.dtos.meofficer.*;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.models.AuditLog;
import com.dseme.app.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ME_OFFICER audit log viewing operations.
 * 
 * Enforces strict partner-level data isolation.
 * Only audit logs related to the partner are visible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MEOfficerAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MEOfficerAuthorizationService meOfficerAuthorizationService;

    /**
     * Gets all audit logs for the partner with filtering and pagination.
     * 
     * @param context ME_OFFICER context
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param actorId Filter by actor ID (optional)
     * @param action Filter by action (optional)
     * @param entityType Filter by entity type (optional)
     * @param startDate Filter by start date (optional)
     * @param endDate Filter by end date (optional)
     * @return Paginated audit log list
     */
    public AuditLogListResponseDTO getAuditLogs(
            MEOfficerContext context,
            int page,
            int size,
            UUID actorId,
            String action,
            String entityType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get all audit logs
        List<AuditLog> allLogs = auditLogRepository.findAll();

        // Filter by partner - only show logs where actor belongs to the partner
        List<AuditLog> partnerLogs = allLogs.stream()
                .filter(log -> {
                    // Check if actor belongs to partner
                    if (log.getActor() != null && log.getActor().getPartner() != null) {
                        return log.getActor().getPartner().getPartnerId().equals(context.getPartnerId());
                    }
                    return false;
                })
                .collect(Collectors.toList());

        // Apply additional filters
        List<AuditLog> filtered = partnerLogs.stream()
                .filter(log -> actorId == null || log.getActor().getId().equals(actorId))
                .filter(log -> action == null || log.getAction().equalsIgnoreCase(action))
                .filter(log -> entityType == null || log.getEntityType().equalsIgnoreCase(entityType))
                .filter(log -> {
                    if (startDate == null && endDate == null) {
                        return true;
                    }
                    Instant logInstant = log.getCreatedAt();
                    LocalDate logDate = logInstant.atZone(ZoneId.systemDefault()).toLocalDate();
                    if (startDate != null && logDate.isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && logDate.isAfter(endDate)) {
                        return false;
                    }
                    return true;
                })
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Most recent first
                .collect(Collectors.toList());

        // Manual pagination
        int start = page * size;
        List<AuditLog> pagedLogs = filtered.stream()
                .skip(start)
                .limit(size)
                .collect(Collectors.toList());

        // Map to DTOs
        List<AuditLogResponseDTO> logDTOs = pagedLogs.stream()
                .map(this::mapToAuditLogResponseDTO)
                .collect(Collectors.toList());

        return AuditLogListResponseDTO.builder()
                .auditLogs(logDTOs)
                .totalElements(filtered.size())
                .totalPages((int) Math.ceil((double) filtered.size() / size))
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    /**
     * Exports audit logs to CSV format.
     * 
     * @param context ME_OFFICER context
     * @param actorId Filter by actor ID (optional)
     * @param action Filter by action (optional)
     * @param entityType Filter by entity type (optional)
     * @param startDate Filter by start date (optional)
     * @param endDate Filter by end date (optional)
     * @return CSV string
     */
    public String exportAuditLogs(
            MEOfficerContext context,
            UUID actorId,
            String action,
            String entityType,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // Validate partner access
        meOfficerAuthorizationService.validatePartnerAccess(context, context.getPartnerId());

        // Get filtered logs (without pagination for export)
        AuditLogListResponseDTO response = getAuditLogs(
                context, 0, Integer.MAX_VALUE, actorId, action, entityType, startDate, endDate);

        // Build CSV
        StringBuilder csv = new StringBuilder();
        csv.append("Audit Log ID,Actor Name,Actor Email,Actor Role,Action,Entity Type,Entity ID,Description,Created At\n");

        for (AuditLogResponseDTO log : response.getAuditLogs()) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,\"%s\",%s\n",
                    log.getAuditLogId(),
                    log.getActorName() != null ? log.getActorName().replace(",", ";") : "",
                    log.getActorEmail() != null ? log.getActorEmail() : "",
                    log.getActorRole() != null ? log.getActorRole() : "",
                    log.getAction() != null ? log.getAction().replace(",", ";") : "",
                    log.getEntityType() != null ? log.getEntityType() : "",
                    log.getEntityId() != null ? log.getEntityId() : "",
                    log.getDescription() != null ? log.getDescription().replace("\"", "\"\"") : "",
                    log.getCreatedAt() != null ? log.getCreatedAt().toString() : ""
            ));
        }

        return csv.toString();
    }

    /**
     * Maps AuditLog entity to AuditLogResponseDTO.
     */
    private AuditLogResponseDTO mapToAuditLogResponseDTO(AuditLog auditLog) {
        String actorName = null;
        String actorEmail = null;
        if (auditLog.getActor() != null) {
            actorName = auditLog.getActor().getFirstName() + " " + auditLog.getActor().getLastName();
            actorEmail = auditLog.getActor().getEmail();
        }

        return AuditLogResponseDTO.builder()
                .auditLogId(auditLog.getId())
                .actorId(auditLog.getActor() != null ? auditLog.getActor().getId() : null)
                .actorName(actorName)
                .actorEmail(actorEmail)
                .actorRole(auditLog.getActorRole())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .description(auditLog.getDescription())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
