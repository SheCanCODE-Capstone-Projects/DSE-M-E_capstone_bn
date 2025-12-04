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
    public String assignRole(@PathVariable UUID id,@Valid @RequestBody RoleRequestDTO request) {
        return userRoleService.requestRole(id, request);
    }
}

