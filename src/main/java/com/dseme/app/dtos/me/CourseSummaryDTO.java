package com.dseme.app.dtos.me;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSummaryDTO {
    private UUID id;
    private String name;
    private String code;
    private String level;
}