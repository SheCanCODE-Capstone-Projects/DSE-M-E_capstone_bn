package com.dseme.app.services.auth;

import com.dseme.app.models.CustomOAuth2User;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.core.ParameterizedTypeReference;

import java.util.*;

/**
 * Custom OAuth2 user service that handles Google OAuth2 user information.
 * 
 * Google's v2 userinfo endpoint returns 'id' instead of 'sub', so we need to
 * handle this properly. We fetch the user info directly and create the OAuth2User
 * with 'id' as the name attribute.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            // Get the user info endpoint URI
            String userInfoUri = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUri();
            
            // Get the access token
            String accessToken = userRequest.getAccessToken().getTokenValue();
            
            // Create request entity with Authorization header
            RequestEntity<?> request = RequestEntity
                    .get(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .build();
            
            // Fetch user info
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    request, 
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> attributes = response.getBody();
            
            if (attributes == null) {
                throw new OAuth2AuthenticationException("Unable to fetch user info from Google");
            }
            
            // Google v2 userinfo returns 'id', not 'sub'
            // Ensure 'sub' exists for compatibility (use 'id' as fallback)
            if (!attributes.containsKey("sub") && attributes.containsKey("id")) {
                attributes.put("sub", attributes.get("id"));
            }
            
            // Get the userNameAttributeName from client registration
            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();
            
            // If userNameAttributeName is 'sub' but we don't have it, use 'id'
            if ("sub".equals(userNameAttributeName) && !attributes.containsKey("sub") && attributes.containsKey("id")) {
                userNameAttributeName = "id";
            }
            
            // Create authorities (Google OAuth2 typically doesn't provide roles)
            Collection<? extends GrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );
            
            // Create DefaultOAuth2User with the attributes
            DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                    authorities,
                    attributes,
                    userNameAttributeName
            );
            
            return new CustomOAuth2User(oauth2User);
            
        } catch (RestClientException e) {
            throw new OAuth2AuthenticationException("Error fetching user info from Google: " + e.getMessage());
        }
    }
}
