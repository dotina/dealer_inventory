package com.dealer.dealer_inventory.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Per-tenant token-bucket rate limiter using Bucket4j.
 * <p>
 * Keyed by {@code X-Tenant-Id} header value. When the bucket is exhausted the filter
 * short-circuits with <b>429 Too Many Requests</b> and a {@code Retry-After} header.
 * </p>
 * <p>
 * Buckets are stored in a Caffeine cache with access-based eviction to prevent
 * unbounded memory growth from short-lived tenant IDs.
 * </p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final long capacity;
    private final long refillPerSecond;
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.bucket-capacity:200}") long capacity,
            @Value("${app.rate-limit.requests-per-second:100}") long refillPerSecond,
            @Value("${app.rate-limit.bucket-idle-timeout-minutes:30}") long bucketIdleTimeoutMinutes,
            @Value("${app.rate-limit.max-buckets:10000}") long maxBuckets) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(bucketIdleTimeoutMinutes))
                .maximumSize(maxBuckets)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Use X-Tenant-Id for per-tenant throttling; fall back to remote IP
        String key = request.getHeader("X-Tenant-Id");
        if (key == null || key.isBlank()) {
            key = request.getRemoteAddr();
        }

        Bucket bucket = buckets.get(key, k -> createBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "1");

            String json = String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"path\":\"%s\"}",
                    Instant.now(), request.getRequestURI());
            response.getWriter().write(json);
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillPerSecond, Duration.ofSeconds(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
