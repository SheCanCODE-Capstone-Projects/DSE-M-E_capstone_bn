package com.dseme.app.services.donor;

import com.dseme.app.dtos.donor.DonorStatisticsDTO;
import com.dseme.app.repositories.MeParticipantRepository;
import com.dseme.app.repositories.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonorService {
    
    private final PartnerRepository partnerRepository;
    private final MeParticipantRepository participantRepository;
    
    public DonorStatisticsDTO getStatistics() {
        long totalPartners = partnerRepository.count();
        long totalParticipants = participantRepository.count();
        
        return DonorStatisticsDTO.builder()
                .totalPartners((int) totalPartners)
                .totalImpacted((int) totalParticipants)
                .avgEmploymentRate(72.0)
                .budgetEfficiency(84.0)
                .build();
    }
}
