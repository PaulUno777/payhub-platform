package com.payhub.orchestrator.infrastructure.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantIsolationFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-PayHub-Tenant-Id";
    public static final String TENANT_CLAIM = "tenant_id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String headerTenant = request.getHeader(TENANT_HEADER);
        if (headerTenant == null || headerTenant.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            filterChain.doFilter(request, response);
            return;
        }
        Jwt jwt = jwtAuth.getToken();
        String claimTenant = jwt.getClaimAsString(TENANT_CLAIM);
        if (claimTenant == null || !normalize(claimTenant).equals(normalize(headerTenant))) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("TENANT_CLAIM_MISMATCH");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String normalize(String value) {
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException ex) {
            return value.trim();
        }
    }
}
