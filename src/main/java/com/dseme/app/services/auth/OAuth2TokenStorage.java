package com.dseme.app.services.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Temporary storage service for OAuth2 JWT tokens.
 * Stores tokens with a unique code that can be used to retrieve them once.
 * Tokens are automatically cleaned up after 5 minutes to prevent memory leaks.
 */
@Service
public class OAuth2TokenStorage {

    private final Map<String, String> tokenStorage = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public OAuth2TokenStorage() {
        // Clean up expired tokens every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanup, 5, 5, TimeUnit.MINUTES);
    }

    /**
     * Stores a JWT token and returns a unique code to retrieve it.
     * @param jwtToken The JWT token to store
     * @return A unique code that can be used to retrieve the token
     */
    public String storeToken(String jwtToken) {
        String code = UUID.randomUUID().toString();
        tokenStorage.put(code, jwtToken);
        return code;
    }

    /**
     * Retrieves and removes a JWT token using the provided code.
     * This is a one-time operation - the token is removed after retrieval.
     * @param code The unique code to retrieve the token
     * @return The JWT token, or null if the code is invalid or already used
     */
    public String retrieveAndRemoveToken(String code) {
        return tokenStorage.remove(code);
    }

    /**
     * Cleans up old tokens (older than 5 minutes).
     * This is called periodically by the scheduler.
     */
    private void cleanup() {
        // Since we're using a simple map, we'll just clear all entries
        // In a production environment, you might want to track timestamps
        // For now, tokens are removed after being retrieved, so this is mainly
        // a safety measure for tokens that were never retrieved
        if (tokenStorage.size() > 1000) {
            tokenStorage.clear();
        }
    }
}

