package com.dseme.app.dtos.me;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortSummaryDTO {
    private UUID id;
    private String name;
    private CourseSummaryDTO course;
}