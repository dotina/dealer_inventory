package com.dealer.dealer_inventory.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMissingTenant_returns400() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dealers");
        ResponseEntity<Map<String, Object>> resp =
                handler.handleMissingTenant(new MissingTenantException(), req);

        assertEquals(400, resp.getStatusCode().value());
        assertEquals("Missing required header: X-Tenant-Id", resp.getBody().get("message"));
        assertEquals("/dealers", resp.getBody().get("path"));
    }

    @Test
    void handleForbidden_returns403() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dealers/123");
        ResponseEntity<Map<String, Object>> resp =
                handler.handleForbidden(new ForbiddenException("Cross-tenant"), req);

        assertEquals(403, resp.getStatusCode().value());
        assertEquals("Cross-tenant", resp.getBody().get("message"));
    }

    @Test
    void handleAccessDenied_returns403() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/dealers");
        ResponseEntity<Map<String, Object>> resp =
                handler.handleAccessDenied(new AccessDeniedException("Denied"), req);

        assertEquals(403, resp.getStatusCode().value());
        assertEquals("Access denied", resp.getBody().get("message"));
    }

    @Test
    void handleNotFound_returns404() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dealers/xyz");
        ResponseEntity<Map<String, Object>> resp =
                handler.handleNotFound(new ResourceNotFoundException("Dealer", "xyz"), req);

        assertEquals(404, resp.getStatusCode().value());
        assertTrue(resp.getBody().get("message").toString().contains("Dealer"));
    }

    @Test
    void handleRateLimit_returns429WithRetryAfter() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dealers");
        ResponseEntity<Map<String, Object>> resp =
                handler.handleRateLimit(new RateLimitExceededException(), req);

        assertEquals(429, resp.getStatusCode().value());
        assertEquals("1", resp.getHeaders().getFirst("Retry-After"));
    }

    @Test
    void handleGeneric_returns500() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dealers");
        ResponseEntity<Map<String, Object>> resp =
                handler.handleGeneric(new RuntimeException("unexpected"), req);

        assertEquals(500, resp.getStatusCode().value());
        assertEquals("Internal server error", resp.getBody().get("message"));
    }

    @Test
    void forbiddenException_defaultMessage() {
        ForbiddenException ex = new ForbiddenException();
        assertEquals("Access denied: cross-tenant access is not allowed", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_formatsMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Vehicle", "abc-123");
        assertTrue(ex.getMessage().contains("Vehicle"));
        assertTrue(ex.getMessage().contains("abc-123"));
    }
}

