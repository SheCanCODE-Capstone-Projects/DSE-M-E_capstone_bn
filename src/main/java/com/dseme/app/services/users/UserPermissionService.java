package com.dseme.app.services.users;

import com.dseme.app.enums.Role;
import com.dseme.app.exceptions.AccessDeniedException;
import com.dseme.app.exceptions.ResourceNotFoundException;
import com.dseme.app.filters.JwtAuthenticationFilter;
import com.dseme.app.models.Notification;
import com.dseme.app.models.RoleRequest;
import com.dseme.app.models.User;
import com.dseme.app.repositories.NotificationRepository;
import com.dseme.app.repositories.UserRepository;
import com.dseme.app.utilities.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UserPermissionService {
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final NotificationRepository notificationRepo;

    public UserPermissionService(UserRepository userRepo,
                                 JwtUtil jwtUtil,
                                 JwtAuthenticationFilter jwtAuthenticationFilter,
                                 NotificationRepository notificationRepo) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.notificationRepo = notificationRepo;
    }

    public User getActor(HttpServletRequest request) {
        String email = jwtUtil.getEmailFromToken(jwtAuthenticationFilter.parseJwt(request));

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));
    }

    public void approveOrRejectRequest(User approver, RoleRequest request) {

        Notification notification = notificationRepo.findByRoleRequestAndRecipient(request, approver)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        // Ensure approver is active
        if (!Boolean.TRUE.equals(approver.getIsActive())) {
            throw new AccessDeniedException("Your account is not active. You cannot approve or reject requests.");
        }

        if(approver.getRole().equals(Role.FACILITATOR) || approver.getRole().equals(Role.UNASSIGNED)){
            throw new AccessDeniedException("You are not allowed to approve or reject this request");
        }

        // Check if no user is trying to approve their own request
        grantUserAccess(approver, notification.getRecipient().getId(), "You are not allowed to approve or reject this request");

//        // Validate approver has authority over the request's scope
//        if (request.getRequestedRole() == Role.FACILITATOR) {
//            // Verify approver is ME_OFFICER or DONOR for the same center
//            if (!approver.getCenter().equals(request.getCenter())) {
//                throw new AccessDeniedException("You are not authorized to approve requests for this center");
//            }
//        } else {
//            // Verify approver is DONOR for the same partner organization
//            if (!approver.getPartner().equals(request.getPartner())) {
//                throw new AccessDeniedException("You are not authorized to approve requests for this partner");
//            }
//        }
    }

    public void allowedToRequestRole(User requester){
        if(!requester.getRole().equals(Role.UNASSIGNED)){
            throw new AccessDeniedException("You already have an approved role");
        }
        
        // Ensure user is active before allowing role request
        if (!Boolean.TRUE.equals(requester.getIsActive())) {
            throw new AccessDeniedException("Your account is not active. Please contact support.");
        }
    }

    public void grantUserAccess(User user, UUID id, String message){
        if(!user.getId().equals(id)) {
            throw new AccessDeniedException(message);
        }
    }

}
