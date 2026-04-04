package com.dealer.dealer_inventory.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    @Test
    void whenDisabled_requestPassesThrough() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false, 200, 100);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void whenEnabled_firstRequestAllowed() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 10, 10);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void whenBucketExhausted_returns429() throws Exception {
        // Capacity of 1, refill 1/sec — second request within same second will fail
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1);
        FilterChain chain = mock(FilterChain.class);

        // First request — consumes the only token
        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/dealers");
        req1.addHeader("X-Tenant-Id", "t1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(req1, res1, chain);
        assertEquals(200, res1.getStatus());

        // Second request — bucket empty
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/dealers");
        req2.addHeader("X-Tenant-Id", "t1");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, chain);

        assertEquals(429, res2.getStatus());
        assertEquals("1", res2.getHeader("Retry-After"));
        assertTrue(res2.getContentAsString().contains("Rate limit exceeded"));
    }

    @Test
    void differentTenants_haveIndependentBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1);
        FilterChain chain = mock(FilterChain.class);

        // Tenant A — consumes its token
        MockHttpServletRequest reqA = new MockHttpServletRequest("GET", "/dealers");
        reqA.addHeader("X-Tenant-Id", "tenantA");
        MockHttpServletResponse resA = new MockHttpServletResponse();
        filter.doFilter(reqA, resA, chain);
        assertEquals(200, resA.getStatus());

        // Tenant B — has its own bucket, should succeed
        MockHttpServletRequest reqB = new MockHttpServletRequest("GET", "/dealers");
        reqB.addHeader("X-Tenant-Id", "tenantB");
        MockHttpServletResponse resB = new MockHttpServletResponse();
        filter.doFilter(reqB, resB, chain);
        assertEquals(200, resB.getStatus());
    }

    @Test
    void missingTenantId_fallsBackToRemoteAddr() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 1);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dealers");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
    }
}

