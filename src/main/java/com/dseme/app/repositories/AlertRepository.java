package com.dseme.app.repositories;

import com.dseme.app.enums.AlertSeverity;
import com.dseme.app.models.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    /**
     * Find all alerts for a partner.
     */
    @Query("SELECT a FROM Alert a WHERE a.partner.partnerId = :partnerId")
    List<Alert> findByPartnerPartnerId(@Param("partnerId") String partnerId);
    
    /**
     * Find unresolved alerts for a partner.
     */
    @Query("SELECT a FROM Alert a WHERE a.partner.partnerId = :partnerId AND a.isResolved = false")
    List<Alert> findUnresolvedByPartnerPartnerId(@Param("partnerId") String partnerId);
    
    /**
     * Find alerts by severity and partner.
     */
    @Query("SELECT a FROM Alert a WHERE a.partner.partnerId = :partnerId AND a.severity = :severity")
    List<Alert> findByPartnerPartnerIdAndSeverity(
            @Param("partnerId") String partnerId,
            @Param("severity") AlertSeverity severity
    );
    
    /**
     * Find alerts by recipient.
     */
    List<Alert> findByRecipientId(UUID recipientId);
    
    /**
     * Find unresolved alerts by recipient.
     */
    List<Alert> findByRecipientIdAndIsResolvedFalse(UUID recipientId);
    
    /**
     * Count unresolved alerts for a partner.
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.partner.partnerId = :partnerId AND a.isResolved = false")
    long countUnresolvedByPartnerPartnerId(@Param("partnerId") String partnerId);
}
