package com.dseme.app.dtos.me;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterDTO {
    private UUID centerId;
    private String centerName;
    private String location;
    private String partnerId;
    private String partnerName;
}
