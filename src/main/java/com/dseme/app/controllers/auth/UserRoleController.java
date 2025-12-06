package com.dseme.app.controllers.auth;

import com.dseme.app.dtos.auth.RoleRequestDTO;
import com.dseme.app.services.auth.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PostMapping("/assign-role/{id}")
    public String assignRole(@PathVariable UUID id,@Valid @RequestBody UUID request) {
        return userRoleService.approveRoleRequest(id,  request);
    }

    @PostMapping("/request-approval/{id}")
    public String requestApproval(@PathVariable UUID id,@Valid @RequestBody RoleRequestDTO request) {
        return userRoleService.requestRoleApproval(id, request);
    }

    @PostMapping("request/approve/{requestId}")
    public String approveRequest( @PathVariable UUID requestId, @RequestParam UUID approverId) {
        return userRoleService.approveRoleRequest(requestId, approverId);
    }

    @PostMapping("request/reject/{requestId}")
    public String rejectRequest( @PathVariable UUID requestId, @RequestParam UUID approverId, @RequestBody String comment) {
        return userRoleService.rejectRoleRequest(requestId, approverId, comment);
    }

}

