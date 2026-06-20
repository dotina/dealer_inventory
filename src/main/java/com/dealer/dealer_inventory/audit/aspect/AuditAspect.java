package com.dealer.dealer_inventory.audit.aspect;

import com.dealer.dealer_inventory.audit.annotation.Audited;
import com.dealer.dealer_inventory.audit.entity.AuditAction;
import com.dealer.dealer_inventory.audit.entity.AuditLog;
import com.dealer.dealer_inventory.audit.service.AuditService;
import com.dealer.dealer_inventory.exception.ForbiddenException;
import com.dealer.dealer_inventory.exception.MissingTenantException;
import com.dealer.dealer_inventory.exception.RateLimitExceededException;
import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AOP aspect that intercepts every {@code @RestController} method invocation,
 * captures metadata and timing, and persists an {@link AuditLog} asynchronously.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private static final Map<String, AuditAction> METHOD_ACTION_MAP = Map.of(
            "POST", AuditAction.CREATE,
            "GET", AuditAction.READ,
            "PATCH", AuditAction.UPDATE,
            "PUT", AuditAction.UPDATE,
            "DELETE", AuditAction.DELETE
    );

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attrs.getRequest();
        HttpServletResponse response = attrs.getResponse();
        long start = System.currentTimeMillis();

        Throwable thrown = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                recordAudit(joinPoint, request, response, duration, thrown);
            } catch (Exception ex) {
                log.warn("Audit recording failed: {}", ex.getMessage());
            }
        }
    }

    private void recordAudit(ProceedingJoinPoint joinPoint,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             long durationMs,
                             Throwable thrown) {

        String httpMethod = request.getMethod();
        String endpoint = request.getRequestURI();
        String tenantId = TenantContext.get();

        // Resolve role from SecurityContext
        String role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse(null);
        }

        // Determine action: prefer @Audited annotation, else infer from HTTP method
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        AuditAction action;
        String entityType = null;

        Audited audited = method.getAnnotation(Audited.class);
        if (audited != null) {
            action = audited.action();
            entityType = audited.entity().isEmpty() ? null : audited.entity();
        } else {
            action = METHOD_ACTION_MAP.getOrDefault(httpMethod, AuditAction.READ);
            // For GET without path variable → LIST
            if ("GET".equals(httpMethod) && !UUID_PATTERN.matcher(endpoint).find()) {
                action = AuditAction.LIST;
            }
        }

        // Infer entity type from endpoint
        if (entityType == null) {
            if (endpoint.contains("/dealers")) entityType = "Dealer";
            else if (endpoint.contains("/vehicles")) entityType = "Vehicle";
        }

        // Extract entity ID from path if present
        UUID entityId = null;
        Matcher matcher = UUID_PATTERN.matcher(endpoint);
        if (matcher.find()) {
            try {
                entityId = UUID.fromString(matcher.group());
            } catch (IllegalArgumentException ignored) { }
        }

        int responseStatus = thrown != null
                ? resolveStatusFromException(thrown)
                : (response != null ? response.getStatus() : 0);

        AuditLog auditLog = AuditLog.builder()
                .tenantId(tenantId)
                .userId(tenantId) // in this header-based auth, tenant acts as user identity
                .role(role)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .responseStatus(responseStatus)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .durationMs(durationMs)
                .build();

        auditService.saveLog(auditLog);
    }

    private int resolveStatusFromException(Throwable thrown) {
        if (thrown instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND.value();
        }
        if (thrown instanceof ForbiddenException || thrown instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN.value();
        }
        if (thrown instanceof MissingTenantException
                || thrown instanceof MethodArgumentNotValidException
                || thrown instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST.value();
        }
        if (thrown instanceof RateLimitExceededException) {
            return HttpStatus.TOO_MANY_REQUESTS.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}

