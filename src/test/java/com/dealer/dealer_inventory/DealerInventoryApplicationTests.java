package com.dealer.dealer_inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test — boots the full application context with H2 in-memory database.
 * Validates the end-to-end request flow including security filters, controllers,
 * services, repositories, and exception handling.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DealerInventoryApplicationTests {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void fullFlow_createAndRetrieveDealer() throws Exception {
        // Create a dealer
        String createResponse = mockMvc.perform(post("/dealers")
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Integration Dealer",
                                  "email": "int@test.com",
                                  "subscriptionType": "PREMIUM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Dealer"))
                .andExpect(jsonPath("$.tenantId").value("integration-tenant"))
                .andReturn().getResponse().getContentAsString();

        // Extract ID (simple parsing)
        String id = extractId(createResponse);

        // Retrieve the dealer
        mockMvc.perform(get("/dealers/{id}", id)
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Dealer"));

        // Cross-tenant: different tenant cannot see this dealer → 404
        mockMvc.perform(get("/dealers/{id}", id)
                        .header("X-Tenant-Id", "other-tenant")
                        .header("X-Role", "USER"))
                .andExpect(status().isNotFound());

        // List dealers
        mockMvc.perform(get("/dealers")
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "USER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Update the dealer
        mockMvc.perform(patch("/dealers/{id}", id)
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Updated Dealer" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Dealer"));

        // Delete the dealer (soft delete)
        mockMvc.perform(delete("/dealers/{id}", id)
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "USER"))
                .andExpect(status().isNoContent());

        // After soft-delete, normal GET returns 404
        mockMvc.perform(get("/dealers/{id}", id)
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "USER"))
                .andExpect(status().isNotFound());

        // GLOBAL_ADMIN can still see the deleted dealer
        mockMvc.perform(get("/admin/dealers/{id}", id)
                        .header("X-Tenant-Id", "integration-tenant")
                        .header("X-Role", "GLOBAL_ADMIN")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void fullFlow_vehicleCrudAndFilters() throws Exception {
        // Create a dealer first
        String dealerResp = mockMvc.perform(post("/dealers")
                        .header("X-Tenant-Id", "vt")
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "VDealer", "email": "vd@test.com", "subscriptionType": "PREMIUM" }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String dealerId = extractId(dealerResp);

        // Create a vehicle
        String vehResp = mockMvc.perform(post("/vehicles")
                        .header("X-Tenant-Id", "vt")
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                { "dealerId": "%s", "model": "Model S", "price": 50000, "status": "AVAILABLE" }
                                """, dealerId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String vehicleId = extractId(vehResp);

        // List with premium filter
        mockMvc.perform(get("/vehicles")
                        .header("X-Tenant-Id", "vt")
                        .header("X-Role", "USER")
                        .param("subscription", "PREMIUM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].model").value("Model S"));

        // Update vehicle status
        mockMvc.perform(patch("/vehicles/{id}", vehicleId)
                        .header("X-Tenant-Id", "vt")
                        .header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "SOLD" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));

        // Delete vehicle
        mockMvc.perform(delete("/vehicles/{id}", vehicleId)
                        .header("X-Tenant-Id", "vt")
                        .header("X-Role", "USER"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCountBySubscription_requiresGlobalAdmin() throws Exception {
        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("X-Tenant-Id", "t1")
                        .header("X-Role", "USER"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("X-Tenant-Id", "t1")
                        .header("X-Role", "GLOBAL_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BASIC").isNumber())
                .andExpect(jsonPath("$.PREMIUM").isNumber());
    }

    @Test
    void missingTenantId_returns400() throws Exception {
        mockMvc.perform(get("/dealers"))
                .andExpect(status().isBadRequest());
    }

    private String extractId(String json) {
        // Simple extraction: "id":"<uuid>"
        int start = json.indexOf("\"id\":\"") + 6;
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
