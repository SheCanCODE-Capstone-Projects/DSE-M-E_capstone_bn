package com.dseme.app.services.auth;

import com.dseme.app.dtos.auth.RoleRequestDTO;
import com.dseme.app.models.Partner;
import com.dseme.app.models.Role;
import com.dseme.app.models.User;
import com.dseme.app.repositories.PartnerRepository;
import com.dseme.app.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserRoleService {

    private final UserRepository userRepo;
    private final PartnerRepository partnerRepo;

    public UserRoleService(UserRepository userRepo, PartnerRepository partnerRepo) {
        this.userRepo = userRepo;
        this.partnerRepo = partnerRepo;
    }

    public String requestRole(UUID userId, RoleRequestDTO request) {
        // Double check if id is not null
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // Check if user requesting role exists
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.UNASSIGNED) {
            throw new RuntimeException("User already has a role assigned.");
        }

        System.out.println("ParterId: "+ request.getPartnerId());
        System.out.println("Role: "+ request.getRequestedRole());

        // Check if the Partner (Organisation) a user belongs to exists
        Partner partner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new RuntimeException("Partner does not exist"));

        System.out.println("Partner: " + partner.getPartnerName());

        System.out.println("Reached here!");
        // Business rules
        Role requested = Role.valueOf(request.getRequestedRole());
        System.out.println("Role requested: " + requested);

        // Count ME officers
        long meCount = partner.getUsers().stream()
                .filter(officer -> officer.getRole() == Role.ME_OFFICER)
                .count();
        System.out.println("MeCount: " + meCount);

        if (requested == Role.ME_OFFICER && meCount > 0) {
            throw new RuntimeException("This partner already has an ME Officer");
        }

        if (requested == Role.FACILITATOR && meCount == 0) {
            throw new RuntimeException("Cannot be a facilitator: Partner has no ME Officer");
        }

        // Assign role
        System.out.println("Assigning role");
        user.setPartner(partner);
        user.setRole(requested);
        userRepo.save(user);

        return user.getEmail() + " is now assigned as " + requested;
    }
}