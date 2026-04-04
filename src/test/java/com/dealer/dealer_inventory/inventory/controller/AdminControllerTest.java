package com.dealer.dealer_inventory.inventory.controller;

import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import com.dealer.dealer_inventory.inventory.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminController. Note: @PreAuthorize is NOT evaluated in standalone
 * MockMvc — role-based access is tested in the integration test (DealerInventoryApplicationTests).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdminControllerTest {

    @Autowired private WebApplicationContext wac;
    @MockitoBean private AdminService adminService;
    private MockMvc mockMvc;

    private static final String TENANT = "test-tenant";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void countBySubscription_returnsMap() throws Exception {
        when(adminService.countBySubscription()).thenReturn(Map.of("BASIC", 5L, "PREMIUM", 3L));

        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Role", "GLOBAL_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.BASIC").value(5))
                .andExpect(jsonPath("$.PREMIUM").value(3));
    }

    @Test
    void countBySubscription_missingTenant_returns400() throws Exception {
        mockMvc.perform(get("/admin/dealers/countBySubscription"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void countBySubscription_asUser_returns403() throws Exception {
        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listDealers_includeDeleted() throws Exception {
        DealerResponse dr = DealerResponse.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).name("Dealer")
                .email("d@test.com").subscriptionType(SubscriptionType.BASIC)
                .deleted(true).deletedAt(Instant.now())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(adminService.listAllDealers(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dr)));

        mockMvc.perform(get("/admin/dealers")
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Role", "GLOBAL_ADMIN")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deleted").value(true));
    }

    @Test
    void getDealerById() throws Exception {
        UUID id = UUID.randomUUID();
        DealerResponse dr = DealerResponse.builder()
                .id(id).tenantId(TENANT).name("D").email("d@t.com")
                .subscriptionType(SubscriptionType.PREMIUM)
                .deleted(true).deletedAt(Instant.now())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(adminService.getDealerById(id, true)).thenReturn(dr);

        mockMvc.perform(get("/admin/dealers/{id}", id)
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Role", "GLOBAL_ADMIN")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));
    }

    @Test
    void listVehicles_includeDeleted() throws Exception {
        VehicleResponse vr = VehicleResponse.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).dealerId(UUID.randomUUID())
                .model("Y").price(new BigDecimal("60000")).status(VehicleStatus.SOLD)
                .deleted(true).deletedAt(Instant.now())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(adminService.listAllVehicles(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vr)));

        mockMvc.perform(get("/admin/vehicles")
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Role", "GLOBAL_ADMIN")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deleted").value(true));
    }

    @Test
    void getVehicleById() throws Exception {
        UUID id = UUID.randomUUID();
        VehicleResponse vr = VehicleResponse.builder()
                .id(id).tenantId(TENANT).dealerId(UUID.randomUUID())
                .model("X").price(BigDecimal.TEN).status(VehicleStatus.AVAILABLE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(adminService.getVehicleById(id, false)).thenReturn(vr);

        mockMvc.perform(get("/admin/vehicles/{id}", id)
                        .header("X-Tenant-Id", TENANT)
                        .header("X-Role", "GLOBAL_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
