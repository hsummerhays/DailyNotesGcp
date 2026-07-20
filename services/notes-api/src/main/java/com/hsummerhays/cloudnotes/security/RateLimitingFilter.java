package com.hsummerhays.cloudnotes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.capacity:100}")
    private double capacity;

    @Value("${app.rate-limit.refill-rate-per-second:10.0}")
    private double refillRate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // We sit behind exactly one trusted reverse proxy (Nginx in Docker Compose, or the
        // GKE Gateway load balancer in prod), which appends its own view of the connecting
        // peer as the LAST entry of X-Forwarded-For. Everything before that is attacker-
        // controlled: a client can freely send "X-Forwarded-For: 1.2.3.4" to mint a fresh
        // bucket on every request. Reading the first entry (rather than the last) would let
        // that request bypass rate limiting entirely, so we must take the last one.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp;
        if (forwardedFor == null || forwardedFor.isBlank()) {
            clientIp = request.getRemoteAddr();
        } else {
            String[] hops = forwardedFor.split(",");
            clientIp = hops[hops.length - 1].trim();
        }

        TokenBucket bucket = buckets.computeIfAbsent(clientIp, ip -> new TokenBucket(capacity, refillRate));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "1");
            response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}");
        }
    }

    // Periodically clean up idle buckets to prevent memory leaks
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanUpIdleBuckets() {
        long oneHourAgo = System.currentTimeMillis() - 3600000;
        buckets.entrySet().removeIf(entry -> entry.getValue().getLastAccessTimestamp() < oneHourAgo);
    }

    public void reset() {
        buckets.clear();
    }

    private static class TokenBucket {
        private final double capacity;
        private final double refillRate;
        private double tokens;
        private long lastRefillTimestamp;
        private long lastAccessTimestamp;

        public TokenBucket(double capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.currentTimeMillis();
            this.lastAccessTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            this.lastAccessTimestamp = now;

            double elapsedSeconds = (now - lastRefillTimestamp) / 1000.0;
            if (elapsedSeconds > 0) {
                tokens = Math.min(capacity, tokens + elapsedSeconds * refillRate);
                lastRefillTimestamp = now;
            }

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized long getLastAccessTimestamp() {
            return lastAccessTimestamp;
        }
    }
}
