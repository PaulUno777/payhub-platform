package com.payhub.gateway.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple in-memory fixed-window rate limit (local/dev). Redis-backed limiter can replace
 * this in a later ticket without changing the Gateway security contract.
 */
@Component
public class InMemoryRateLimitFilter extends OncePerRequestFilter {

    private final int limit;
    private final Duration window;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public InMemoryRateLimitFilter(
            @Value("${payhub.gateway.rate-limit.requests-per-window:120}") int limit,
            @Value("${payhub.gateway.rate-limit.window:PT1M}") Duration window
    ) {
        this.limit = limit;
        this.window = window;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, java.io.IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = clientKey(request);
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.windowStart.plus(window).isBefore(now)) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (counter.count.get() > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class WindowCounter {
        final Instant windowStart;
        final AtomicInteger count;

        WindowCounter(Instant windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
