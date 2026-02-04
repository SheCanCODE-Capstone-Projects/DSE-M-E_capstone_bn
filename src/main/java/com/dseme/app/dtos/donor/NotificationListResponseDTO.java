package com.dseme.app.dtos.donor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for paginated notification list response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListResponseDTO {

    /**
     * List of notifications.
     */
    private List<NotificationSummaryDTO> notifications;

    /**
     * Total number of notifications.
     */
    private Long totalCount;

    /**
     * Number of unread notifications.
     */
    private Long unreadCount;

    /**
     * Current page number (0-based).
     */
    private int page;

    /**
     * Page size.
     */
    private int size;

    /**
     * Total number of pages.
     */
    private int totalPages;
}
