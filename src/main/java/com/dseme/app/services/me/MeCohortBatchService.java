package com.dseme.app.services.me;

import com.dseme.app.dtos.me.CohortBatchResponseDTO;
import com.dseme.app.dtos.me.CreateCohortBatchDTO;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.Center;
import com.dseme.app.models.MeCohortBatch;
import com.dseme.app.models.User;
import com.dseme.app.repositories.CenterRepository;
import com.dseme.app.repositories.MeCohortBatchRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeCohortBatchService {

    private final MeCohortBatchRepository batchRepository;
    private final CenterRepository centerRepository;
    private final UserRepository userRepository;

    public Page<CohortBatchResponseDTO> getAllBatches(Pageable pageable) {
        User currentUser = getCurrentUser();
        
        if (currentUser.getPartner() != null) {
            List<MeCohortBatch> batches = batchRepository.findByPartner_PartnerId(
                    currentUser.getPartner().getPartnerId());
            List<CohortBatchResponseDTO> dtos = batches.stream()
                    .map(this::mapToDTO)
                    .toList();
            return new PageImpl<>(dtos, pageable, dtos.size());
        }
        
        return batchRepository.findAll(pageable).map(this::mapToDTO);
    }

    public List<CohortBatchResponseDTO> getAllBatchesList() {
        User currentUser = getCurrentUser();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MeCohortBatchService.class);
        
        logger.info("Fetching cohort batches for user: {} ({})",
                currentUser.getEmail(),
                currentUser.getPartner() != null ? currentUser.getPartner().getPartnerName() : "NO PARTNER");
        
        if (currentUser.getPartner() != null) {
            List<MeCohortBatch> batches = batchRepository.findByPartner_PartnerId(
                    currentUser.getPartner().getPartnerId());
            logger.info("Found {} cohort batches for organization: {}",
                    batches.size(),
                    currentUser.getPartner().getPartnerName());
            return batches.stream()
                    .map(this::mapToDTO)
                    .toList();
        }
        
        logger.info("User has no partner, returning all batches");
        return batchRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    public CohortBatchResponseDTO getBatchById(UUID id) {
        User currentUser = getCurrentUser();
        MeCohortBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort batch not found"));
        
        if (!belongsToSameOrganization(batch, currentUser)) {
            throw new ResourceNotFoundException("Cohort batch not found");
        }
        
        return mapToDTO(batch);
    }

    @Transactional
    public CohortBatchResponseDTO createBatch(CreateCohortBatchDTO dto) {
        User currentUser = getCurrentUser();
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MeCohortBatchService.class);
        
        logger.info("Creating cohort batch '{}' for user: {} ({})",
                dto.getName(),
                currentUser.getEmail(),
                currentUser.getPartner() != null ? currentUser.getPartner().getPartnerName() : "NO PARTNER");
        
        if (currentUser.getPartner() == null) {
            throw new IllegalStateException("User must be associated with an organization");
        }
        
        Center center = null;
        if (dto.getCenterId() != null) {
            center = centerRepository.findById(dto.getCenterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Center not found"));
        }

        MeCohortBatch batch = MeCohortBatch.builder()
                .name(dto.getName())
                .partner(currentUser.getPartner())
                .center(center)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(CohortStatus.PLANNED)
                .build();

        batch = batchRepository.save(batch);
        logger.info("Cohort batch created successfully with ID: {} for organization: {}",
                batch.getId(),
                batch.getPartner().getPartnerName());
        return mapToDTO(batch);
    }

    @Transactional
    public CohortBatchResponseDTO updateBatchStatus(UUID id, CohortStatus newStatus) {
        MeCohortBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cohort batch not found"));
        
        User currentUser = getCurrentUser();
        if (!belongsToSameOrganization(batch, currentUser)) {
            throw new ResourceNotFoundException("Cohort batch not found");
        }
        
        batch.setStatus(newStatus);
        batch = batchRepository.save(batch);
        
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MeCohortBatchService.class);
        logger.info("Cohort batch {} status updated to: {}", batch.getName(), newStatus);
        
        return mapToDTO(batch);
    }

    private boolean belongsToSameOrganization(MeCohortBatch batch, User currentUser) {
        if (currentUser.getPartner() == null) {
            return true;
        }
        
        if (batch.getPartner() != null) {
            return batch.getPartner().getPartnerId()
                    .equals(currentUser.getPartner().getPartnerId());
        }
        
        return false;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CohortBatchResponseDTO mapToDTO(MeCohortBatch b) {
        return CohortBatchResponseDTO.builder()
                .id(b.getId())
                .name(b.getName())
                .centerId(b.getCenter() != null ? b.getCenter().getId() : null)
                .centerName(b.getCenter() != null ? b.getCenter().getCenterName() : null)
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .build();
    }
}

