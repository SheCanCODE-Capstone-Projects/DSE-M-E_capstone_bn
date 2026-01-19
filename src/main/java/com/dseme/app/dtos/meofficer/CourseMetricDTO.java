package com.dseme.app.dtos.meofficer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Course distribution metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMetricDTO {
    /**
     * Course category name (e.g., "Business Skills").
     */
    private String courseCategory;
    
    /**
     * Number of participants enrolled in this course.
     */
    private Integer enrolledCount;
    
    /**
     * Number of facilitators assigned to this course.
     */
    private Integer assignedFacilitators;
}
