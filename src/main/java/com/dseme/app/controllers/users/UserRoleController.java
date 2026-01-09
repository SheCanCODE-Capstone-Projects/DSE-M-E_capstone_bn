package com.dseme.app.controllers.users;

import com.dseme.app.dtos.users.RejectRequestDTO;
import com.dseme.app.dtos.users.RoleRequestDTO;
import com.dseme.app.services.users.UserRoleService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/request/role")
    public String requestApproval(HttpServletRequest actor, @Valid @RequestBody RoleRequestDTO roleRequestDTO) {
        return userRoleService.requestRoleApproval(actor, roleRequestDTO);
    }

    @PostMapping("/request/approve/{requestId}")
    public String approveRequest( HttpServletRequest actor, @PathVariable UUID requestId) {
        return userRoleService.approveRoleRequest(actor, requestId);
    }

    @PostMapping("/request/reject/{requestId}")
    public String rejectRequest(HttpServletRequest actor, @PathVariable UUID requestId, @Valid @RequestBody RejectRequestDTO rejectRequestDTO) {
        return userRoleService.rejectRoleRequest(actor, requestId, rejectRequestDTO.getComment());
    }

}

