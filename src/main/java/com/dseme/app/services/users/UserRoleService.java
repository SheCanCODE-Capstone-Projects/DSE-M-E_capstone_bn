package com.dseme.app.services.users;

import com.dseme.app.dtos.users.RoleRequestDTO;
import com.dseme.app.enums.Priority;
import com.dseme.app.enums.RequestStatus;
import com.dseme.app.exceptions.ResourceAlreadyExistsException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.models.*;
import com.dseme.app.enums.Role;
import com.dseme.app.repositories.*;
import com.dseme.app.services.notifications.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.Nullable;
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
    private final UserPermissionService userPermissionService;

    public UserRoleService(UserRepository userRepo,
                           PartnerRepository partnerRepo,
                           NotificationService notificationService,
                           CenterRepository centerRepo,
                           RoleRequestRepository roleRequestRepo,
                           UserPermissionService userPermissionService) {
        this.userRepo = userRepo;
        this.partnerRepo = partnerRepo;
        this.notificationService = notificationService;
        this.centerRepo = centerRepo;
        this.roleRequestRepo = roleRequestRepo;
        this.userPermissionService = userPermissionService;
    }

    // Request Role and Permissions from suitable Admins
    public String requestRoleApproval(HttpServletRequest actor, RoleRequestDTO roleRequestDTO){

        User requester = userPermissionService.getActor(actor);

        // Check if they don't have an already assigned role
        userPermissionService.allowedToRequestRole(requester);

        Partner targetPartner = partnerRepo.findById(roleRequestDTO.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner does not exist"));

        Center targetCenter = centerRepo.findByIdAndPartner_PartnerId(roleRequestDTO.getCenterId(), targetPartner.getPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("This center location is does not belong to " + targetPartner.getPartnerName()));

        Role requestedRole;
        try {
                requestedRole = Role.valueOf(roleRequestDTO.getRequestedRole());
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Invalid role: " + roleRequestDTO.getRequestedRole());
            }

        boolean checkDuplicates = roleRequestRepo.existsByRequesterIdAndRequestedRoleAndPartnerPartnerIdAndCenterIdAndStatus(
                requester.getId(),
                requestedRole,
                targetPartner.getPartnerId(),
                targetCenter.getId(),
                RequestStatus.PENDING
        );

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
    public String approveRoleRequest(HttpServletRequest actor,UUID requestId) {

        RoleRequest request = roleRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        //Check if one is allowed to approve or reject a request
        userPermissionService.approveOrRejectRequest(userPermissionService.getActor(actor), request);

        if(!request.getStatus().equals(RequestStatus.PENDING)){
            throw new ResourceAlreadyExistsException("Request already processed!");
        }

        //Updating request Status
        changeRequestStatus(request, userPermissionService.getActor(actor), RequestStatus.APPROVED, null);

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
    public String rejectRoleRequest(HttpServletRequest actor, UUID requestId, String comment) {

        RoleRequest request = roleRequestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        //Check if one is allowed to approve or reject a request
        userPermissionService.approveOrRejectRequest(userPermissionService.getActor(actor), request);

        if(!request.getStatus().equals(RequestStatus.PENDING)){
            throw new ResourceAlreadyExistsException("Request already processed!");
        }

        //Updating request Status
        changeRequestStatus(request, userPermissionService.getActor(actor), RequestStatus.REJECTED, comment);

        // Mark notification as processed by turning is_read to true
        notificationService.markNotificationAsRead(requestId);

        // Notify requester
        String title = "Role Request Rejected";
        String message = "Your request for role '" + request.getRequestedRole() + "' has been rejected. Reason: " + comment;

        notificationService.sendInfoNotification(request.getRequester(), title, message, Priority.HIGH);

        return "Request was successfully rejected.";
    }

    //Changing the Request Status from pending to Approved or Rejected depending on Admins decision
    private void changeRequestStatus(RoleRequest request, User approver, RequestStatus status, @Nullable String comment) {
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
    // Only returns active users who can approve requests
    private List<User> requestApprovers(Role approverRole, Partner targetPartner, Center targetCenter,  Role roleRequest) {

        if(roleRequest != Role.FACILITATOR) {
            return userRepo.findAll().stream()
                    .filter(user -> user.getRole() == approverRole
                            && Boolean.TRUE.equals(user.getIsActive())) // Only active users can approve
                    .toList();
        }

        return userRepo.findAll().stream()
                .filter(user -> user.getRole() == approverRole
                        && Boolean.TRUE.equals(user.getIsActive()) // Only active users can approve
                        && user.getPartner() != null && user.getPartner().getPartnerId().equals(targetPartner.getPartnerId())
                        && user.getCenter() != null && user.getCenter().getId().equals(targetCenter.getId()))
                .toList();
    }
}