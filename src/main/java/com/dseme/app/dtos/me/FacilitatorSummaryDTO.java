package com.dseme.app.dtos.me;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorSummaryDTO {
    private UUID id;
    private String firstName;
    private String lastName;
}