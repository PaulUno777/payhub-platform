package com.payhub.opsbff.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Tag("unit")
class TenantIsolationFilterTest {

    private final TenantIsolationFilter filter = new TenantIsolationFilter();

    @Test
    void should_pass_when_header_matches_claim() throws Exception {
        UUID tenant = UUID.fromString("11111111-1111-1111-1111-111111111111");
        setJwt(tenant.toString());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantIsolationFilter.TENANT_HEADER, tenant.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_forbid_when_header_mismatches_claim() throws Exception {
        setJwt("11111111-1111-1111-1111-111111111111");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TenantIsolationFilter.TENANT_HEADER, "22222222-2222-2222-2222-222222222222");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEqualTo("TENANT_CLAIM_MISMATCH");
        SecurityContextHolder.clearContext();
    }

    private static void setJwt(String tenantId) {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("user")
                .claim("tenant_id", tenantId)
                .audience(List.of("payhub"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
