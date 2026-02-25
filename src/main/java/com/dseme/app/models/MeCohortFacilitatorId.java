package com.dseme.app.models;

import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeCohortFacilitatorId implements Serializable {
    private UUID cohortId;
    private UUID facilitatorId;
}
