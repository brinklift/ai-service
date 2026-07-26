package com.blift.aiservice;

import jakarta.servlet.http.HttpServletRequest;

public final class AiServiceUtils {

    private AiServiceUtils() {
    }

    public static String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or malformed Authorization header");
        }
        return authHeader.substring(7);
    }
}
