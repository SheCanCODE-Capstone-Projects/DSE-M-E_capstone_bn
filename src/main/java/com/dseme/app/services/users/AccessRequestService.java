package com.dseme.app.services.users;

import com.dseme.app.dtos.users.AccessRequestResponseDTO;
import com.dseme.app.dtos.users.RoleRequestDTO;
import com.dseme.app.enums.RequestStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.AccessRequest;
import com.dseme.app.models.User;
import com.dseme.app.repositories.AccessRequestRepository;
import com.dseme.app.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccessRequestResponseDTO createRoleRequest(RoleRequestDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = auth.getName();
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AccessRequest request = AccessRequest.builder()
                .requesterEmail(user.getEmail())
                .requesterName(user.getFirstName() + " " + user.getLastName())
                .requestedRole(Role.valueOf(dto.getRequestedRole().toUpperCase()))
                .reason(dto.getReason())
                .status(RequestStatus.PENDING)
                .build();

        request = accessRequestRepository.save(request);
        return mapToResponseDTO(request);
    }

    public Page<AccessRequestResponseDTO> getAllRequests(Pageable pageable) {
        return accessRequestRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    public Page<AccessRequestResponseDTO> getPendingRequests(Pageable pageable) {
        return accessRequestRepository.findByStatus(RequestStatus.PENDING, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional
    public AccessRequestResponseDTO approveRequest(UUID requestId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request has already been processed");
        }

        // Update the user's role
        User user = userRepository.findByEmail(request.getRequesterEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));
        
        user.setRole(request.getRequestedRole());
        userRepository.save(user);

        // Update the request
        request.setStatus(RequestStatus.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);

        request = accessRequestRepository.save(request);
        return mapToResponseDTO(request);
    }

    @Transactional
    public AccessRequestResponseDTO rejectRequest(UUID requestId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminEmail = auth.getName();
        
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Access request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);

        request = accessRequestRepository.save(request);
        return mapToResponseDTO(request);
    }

    private AccessRequestResponseDTO mapToResponseDTO(AccessRequest request) {
        return AccessRequestResponseDTO.builder()
                .id(request.getId())
                .requesterEmail(request.getRequesterEmail())
                .requesterName(request.getRequesterName())
                .requestedRole(request.getRequestedRole().name())
                .reason(request.getReason())
                .status(request.getStatus().name())
                .requestedAt(request.getRequestedAt())
                .reviewedAt(request.getReviewedAt())
                .reviewedBy(request.getReviewedBy() != null ? 
                    request.getReviewedBy().getFirstName() + " " + request.getReviewedBy().getLastName() : null)
                .build();
    }
}