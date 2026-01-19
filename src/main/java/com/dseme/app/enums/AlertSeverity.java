package com.dseme.app.enums;

/**
 * Enum for alert severity levels.
 * Used to determine UI styling (Red/Yellow/Blue).
 */
public enum AlertSeverity {
    /**
     * Critical alerts (Red) - require immediate action.
     * Maps to "Review Now" endpoint.
     */
    CRITICAL,
    
    /**
     * Warning alerts (Yellow) - require investigation.
     * Maps to "Investigate" endpoint.
     */
    WARNING,
    
    /**
     * Info alerts (Blue) - informational notifications.
     * Maps to "Send" or "Acknowledge" endpoint.
     */
    INFO
}
