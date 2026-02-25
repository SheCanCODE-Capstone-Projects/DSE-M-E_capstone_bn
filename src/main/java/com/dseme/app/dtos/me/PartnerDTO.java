package com.dseme.app.dtos.me;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerDTO {
    private String partnerId;
    private String partnerName;
    private String country;
    private String region;
}
