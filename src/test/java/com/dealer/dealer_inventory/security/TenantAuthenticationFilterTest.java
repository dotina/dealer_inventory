package com.dealer.dealer_inventory.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAuthenticationFilterTest {

    private final TenantAuthenticationFilter filter = new TenantAuthenticationFilter();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingTenantId_returns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("Missing required header"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void blankTenantId_returns400() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        request.addHeader("X-Tenant-Id", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validTenantAndRole_setsContextAndProceedsChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        request.addHeader("X-Tenant-Id", "tenant-1");
        request.addHeader("X-Role", "GLOBAL_ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        // Context should be cleared after filter completes
        assertNull(TenantContext.get());
    }

    @Test
    void missingRole_defaultsToUSER() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture auth during chain execution
        FilterChain chain = (req, res) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        };

        filter.doFilter(request, response, chain);
    }

    @Test
    void optionsPreflight_passesThroughWithoutTenantCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/dealers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void validTenantSetsAuthenticationWithCorrectRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dealers");
        request.addHeader("X-Tenant-Id", "t-abc");
        request.addHeader("X-Role", "GLOBAL_ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals("t-abc", auth.getPrincipal());
            assertTrue(auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_GLOBAL_ADMIN")));
            assertEquals("t-abc", TenantContext.get());
        };

        filter.doFilter(request, response, chain);
    }
}

