package com.example.skeleton.security;

import com.example.skeleton.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (isSkippedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }


        if (securityProperties.isDevMode()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication");
            return;
        }

        if (isAuthenticatedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);

        if (role == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No role assigned");
            return;
        }

        if (!isRoleAllowed(role, path)) {
            log.warn("Access denied: role={} path={}", role, path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSkippedPath(String path) {
        return securityProperties.getPathsToSkip().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isAuthenticatedPath(String path) {
        return securityProperties.getAuthenticatedPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isRoleAllowed(String role, String path) {
        List<String> allowedPaths = securityProperties.getRolePaths().get(role);
        if (allowedPaths == null) {
            return false;
        }

        return allowedPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}