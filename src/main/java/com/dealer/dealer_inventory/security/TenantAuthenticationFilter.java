package com.dealer.dealer_inventory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Extracts {@code X-Tenant-Id} and {@code X-Role} headers from every request.
 * <ul>
 *   <li>Missing {@code X-Tenant-Id} → immediate <b>400</b>.</li>
 *   <li>{@code X-Role} is mapped to a Spring Security authority ({@code ROLE_<value>})
 *       so that {@code @PreAuthorize("hasRole('GLOBAL_ADMIN')")} works out-of-the-box.</li>
 * </ul>
 */
@Component
public class TenantAuthenticationFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Allow CORS preflight through without headers
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isBlank()) {
            writeError(response, HttpStatus.BAD_REQUEST, "Missing required header: X-Tenant-Id",
                    request.getRequestURI());
            return;
        }

        // Store tenant in ThreadLocal
        TenantContext.set(tenantId.trim());

        try {
            // Build authentication from X-Role header
            String role = request.getHeader("X-Role");
            if (role == null || role.isBlank()) {
                role = "USER"; // default role
            }

            var authority = new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase());
            var authentication = new UsernamePasswordAuthenticationToken(
                    tenantId, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status,
                            String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now(), status.value(), status.getReasonPhrase(), message, path);
        response.getWriter().write(json);
    }
}

