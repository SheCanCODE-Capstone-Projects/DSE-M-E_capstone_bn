package com.dseme.app.enums;

/**
 * Enum for bulk participant action types.
 */
public enum ParticipantBulkActionType {
    /**
     * Send reminder notifications to participants.
     */
    SEND_REMINDER,
    
    /**
     * Change participants' cohort assignment.
     */
    CHANGE_COHORT,
    
    /**
     * Export participant data.
     */
    EXPORT_DATA,
    
    /**
     * Archive participants (soft delete or mark as inactive).
     */
    ARCHIVE,
    
    /**
     * Bulk update participant profiles.
     */
    BULK_UPDATE,
    
    /**
     * Bulk approve/reject enrollments.
     */
    BULK_ENROLLMENT_APPROVAL
}
