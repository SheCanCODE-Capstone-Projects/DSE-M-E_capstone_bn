package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.RoleRequestDTO;
import com.dseme.app.enums.Priority;
import com.dseme.app.enums.RequestStatus;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.enums.Role;
import com.dseme.app.repositories.*;
import com.dseme.app.services.notifications.NotificationService;
import jakarta.validation.constraints.Null;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserRoleService {

    private final UserRepository userRepo;
    private final PartnerRepository partnerRepo;
    private final CenterRepository centerRepo;
    private final RoleRequestRepository roleRequestRepo;
    private final NotificationService notificationService;

    public UserRoleService(UserRepository userRepo,
                           PartnerRepository partnerRepo,
                           NotificationService notificationService,
                           CenterRepository centerRepo,
                           RoleRequestRepository roleRequestRepo) {
        this.userRepo = userRepo;
        this.partnerRepo = partnerRepo;
        this.notificationService = notificationService;
        this.centerRepo = centerRepo;
        this.roleRequestRepo = roleRequestRepo;
    }
    

    // Request Role and Permissions from suitable Admins
    public String requestRoleApproval(UUID userId, RoleRequestDTO request){

        User requester = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Partner targetPartner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner does not exist"));

        Center targetCenter = centerRepo.findByIdAndPartner_PartnerId(request.getCenterId(), targetPartner.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("This Center Location is does not belong to " + targetPartner.getPartnerName()));

        Role requestedRole = Role.valueOf(request.getRequestedRole());

        boolean checkDuplicates = roleRequestRepo.existsByRequesterIdAndRequestedRoleAndPartnerPartnerIdAndCenterId(requester.getId(), requestedRole, targetPartner.getPartnerId(), targetCenter.getId());

        if (checkDuplicates) {
            throw new ResourceAlreadyExistsException("This request already exists!");
        }

        RoleRequest roleRequested = saveRoleRequest(requester, targetPartner, targetCenter, requestedRole);

        List<User> potentialApprovers;

        if( requestedRole == Role.FACILITATOR){
            potentialApprovers = requestApprovers(Role.ME_OFFICER, targetPartner, targetCenter, requestedRole);

        } else {
            potentialApprovers = requestApprovers(Role.PARTNER, targetPartner, targetCenter, requestedRole);
        }

        if(potentialApprovers.isEmpty()){
            throw new ResourceNotFoundException("Couldn't find anyone suitable to approve your request.");
        }

        String title = "Approval Request";
        String message = "User with email : '" + requester.getEmail() +
                "' wants approval for the role: " + requestedRole +
                " within partner organization: " + targetPartner.getPartnerName() +
                " at center: " + targetCenter.getCenterName() +
                " at location: " + targetCenter.getLocation();

        // Send notification to all suitable Partners in that Organisation
        for (User suitableApprover : potentialApprovers) {
            notificationService.sendApprovalNotification(suitableApprover, roleRequested, title, message);
        }

        return "Your Request was successfully sent!";
    }

    // Approving a Role Request and Updating all related tables
    public String approveRoleRequest(UUID requestId, UUID approverId) {

        User approver = userRepo.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RoleRequest request = roleRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if(!request.getStatus().equals(RequestStatus.PENDING)){
            throw new ResourceAlreadyExistsException("Request already processed!");
        }

        //Updating request Status
        changeRequestStatus(request, approver, RequestStatus.APPROVED, null);

        //Update User Role, Partner (Organisation) and Center(Location)
        User requester = assignRoleToRequester(request);

        // Mark notification as processed by turning is_read to true
        notificationService.markNotificationAsRead(requestId);

        // Notify requester
        String title = "Role Request Approved";
        String message = "Your request for role '" + request.getRequestedRole() + "' has been approved.";

        notificationService.sendInfoNotification(requester, title, message, Priority.LOW);

        return "Request was successfully approved.";
    }

    // Rejecting a Role Request and notifying the requester
    public String rejectRoleRequest(UUID requestId, UUID approverId, String comment) {

        User approver = userRepo.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RoleRequest request = roleRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if(!request.getStatus().equals(RequestStatus.PENDING)){
            throw new ResourceAlreadyExistsException("Request already processed!");
        }

        //Updating request Status
        changeRequestStatus(request, approver, RequestStatus.REJECTED, comment);

        // Notify requester
        String title = "Role Request Rejected";
        String message = "Your request for role '" + request.getRequestedRole() + "' has been rejected. Reason: " + comment;

        notificationService.sendInfoNotification(request.getRequester(), title, message, Priority.HIGH);

        return "Request was successfully rejected.";
    }

    //Changing the Request Status from pending to Approved or Rejected depending on Admins decision
    private void changeRequestStatus(RoleRequest request, User approver, RequestStatus status, @Null String comment) {
        request.setStatus(status);
        request.setApprovedBy(approver);
        request.setApprovedAt(Instant.now());
        request.setAdminComment(comment);
        roleRequestRepo.save(request);
    }

    //Updating the User record by adding role, partner and center information
    private User assignRoleToRequester(RoleRequest request) {
        User requester = request.getRequester();
        requester.setRole(request.getRequestedRole());
        requester.setPartner(request.getPartner());
        requester.setCenter(request.getCenter());
        userRepo.save(requester);
        return requester;
    }

    // Saving the requested role to the role_request table
    private RoleRequest saveRoleRequest(User requester, Partner targetPartner, Center targetCenter, Role requestedRole) {
        RoleRequest roleRequest = new RoleRequest();

        roleRequest.setRequester(requester);
        roleRequest.setPartner(targetPartner);
        roleRequest.setCenter(targetCenter);
        roleRequest.setRequestedRole(requestedRole);
        roleRequest.setStatus(RequestStatus.PENDING);

        return roleRequestRepo.save(roleRequest);
    }

    // Getting a list of potential Approvers
    private List<User> requestApprovers(Role approverRole, Partner targetPartner, Center targetCenter,  Role roleRequest) {

        if(roleRequest != Role.FACILITATOR) {
            return userRepo.findAll().stream()
                    .filter(user -> user.getRole() == approverRole)
                    .toList();
        }

        return userRepo.findAll().stream()
                .filter(user -> user.getRole() == approverRole && user.getPartner() == targetPartner && user.getCenter() == targetCenter)
                .toList();
    }
}