package com.dseme.app.dtos.facilitator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAttendanceResponseDTO {
    private String message;
    private int recordedCount;
    private int updatedCount;
}
