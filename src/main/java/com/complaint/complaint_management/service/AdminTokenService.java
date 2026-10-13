
package com.complaint.complaint_management.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminTokenService {

    private static final long TOKEN_LIFETIME_MS =
            8L * 60 * 60 * 1000;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, Long> tokens =
            new ConcurrentHashMap<>();

    public String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        tokens.put(token, System.currentTimeMillis() + TOKEN_LIFETIME_MS);
        return token;
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Long expiry = tokens.get(token);

        if (expiry == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expiry) {
            tokens.remove(token);
            return false;
        }

        return true;
    }

    public void invalidate(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
