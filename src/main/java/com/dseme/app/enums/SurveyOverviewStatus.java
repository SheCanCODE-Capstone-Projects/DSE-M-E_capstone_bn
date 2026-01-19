package com.dseme.app.enums;

/**
 * Enum for survey overview status (simplified from SurveyStatus).
 * Maps to SurveyStatus for display purposes.
 */
public enum SurveyOverviewStatus {
    /**
     * Survey is active and accepting responses (PUBLISHED).
     */
    ACTIVE,
    
    /**
     * Survey is pending/not yet published (DRAFT).
     */
    PENDING,
    
    /**
     * Survey is completed/closed (CLOSED).
     */
    COMPLETED
}
