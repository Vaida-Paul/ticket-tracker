package com.example.skeleton.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Jwt getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        Jwt jwt = getCurrentJwt();
        return jwt == null ? null : jwt.getClaim("userId");
    }

    public static String getCurrentUserEmail() {
        Jwt jwt = getCurrentJwt();
        return jwt == null ? null : jwt.getSubject();
    }

    public static String getCurrentUserRole() {
        Jwt jwt = getCurrentJwt();
        return jwt == null ? null : jwt.getClaim("role");
    }

    public static boolean isAuthenticated() {
        return getCurrentJwt() != null;
    }
}