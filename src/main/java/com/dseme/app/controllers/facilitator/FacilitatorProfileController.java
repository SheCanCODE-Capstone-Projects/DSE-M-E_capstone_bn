package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.FacilitatorContext;
import com.dseme.app.dtos.facilitator.FacilitatorProfileDTO;
import com.dseme.app.models.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facilitator/profile")
@RequiredArgsConstructor
public class FacilitatorProfileController extends FacilitatorBaseController {

    @GetMapping
    public ResponseEntity<FacilitatorProfileDTO> getProfile(HttpServletRequest request) {
        FacilitatorContext context = getFacilitatorContext(request);
        User user = context.getFacilitator();
        
        FacilitatorProfileDTO profile = FacilitatorProfileDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .organizationName(user.getPartner() != null ? user.getPartner().getPartnerName() : null)
                .centerName(user.getCenter() != null ? user.getCenter().getCenterName() : null)
                .build();
        
        return ResponseEntity.ok(profile);
    }
}
