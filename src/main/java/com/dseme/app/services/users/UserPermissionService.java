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
import com.dseme.app.utilies.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

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

        Notification notification = notificationRepo.findByRoleRequestAndAndRecipient(request, approver)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));


        if(approver.getRole().equals(Role.FACILITATOR) || approver.getRole().equals(Role.UNASSIGNED)){
            throw new AccessDeniedException("You are not allowed to approve or reject this request");
        }

        grantUserAccess(approver, notification.getRecipient().getId(), "You are not allowed to approve or reject this request");

    }

    public void allowedToRequestRole(User requester){
        if(!requester.getRole().equals(Role.UNASSIGNED)){
            throw new AccessDeniedException("You already have an approved role");
        }
    }

    public void grantUserAccess(User user, UUID id, String message){
        if(!user.getId().equals(id)) {
            throw new AccessDeniedException(message);
        }
    }

}
