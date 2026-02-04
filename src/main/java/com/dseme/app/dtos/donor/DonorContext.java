package com.dseme.app.dtos.donor;

import com.dseme.app.models.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Context object for DONOR role operations.
 * 
 * Contains user information for DONOR users.
 * Unlike ME_OFFICER, DONOR does not have partnerId restriction
 * as they have portfolio-wide access across all partners.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonorContext {
    
    /**
     * DONOR user ID.
     */
    private UUID userId;
    
    /**
     * DONOR user email.
     */
    private String email;
    
    /**
     * DONOR user's full name.
     */
    private String fullName;
    
    /**
     * Reference to the User entity.
     */
    private User user;
}
