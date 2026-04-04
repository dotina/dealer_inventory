package com.dealer.dealer_inventory.inventory.controller;

import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import com.dealer.dealer_inventory.inventory.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class VehicleControllerTest {

    @Autowired private WebApplicationContext wac;
    @MockitoBean private VehicleService vehicleService;
    private MockMvc mockMvc;

    private static final String TENANT = "test-tenant";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    private VehicleResponse sampleResponse(UUID id) {
        return VehicleResponse.builder()
                .id(id).tenantId(TENANT).dealerId(UUID.randomUUID())
                .model("Model S").price(new BigDecimal("50000.00"))
                .status(VehicleStatus.AVAILABLE)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    @Test
    void missingTenantId_returns400() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVehicle_success() throws Exception {
        UUID id = UUID.randomUUID();
        UUID dealerId = UUID.randomUUID();
        when(vehicleService.create(any())).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/vehicles")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                { "dealerId": "%s", "model": "Model S", "price": 50000.00, "status": "AVAILABLE" }
                                """, dealerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.model").value("Model S"));
    }

    @Test
    void createVehicle_missingModel_returns400() throws Exception {
        mockMvc.perform(post("/vehicles")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                { "dealerId": "%s", "price": 50000.00, "status": "AVAILABLE" }
                                """, UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createVehicle_zeroPrice_returns400() throws Exception {
        mockMvc.perform(post("/vehicles")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                { "dealerId": "%s", "model": "X", "price": 0, "status": "AVAILABLE" }
                                """, UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getVehicleById_success() throws Exception {
        UUID id = UUID.randomUUID();
        when(vehicleService.getById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/vehicles/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void listVehicles_noFilters() throws Exception {
        when(vehicleService.list(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(UUID.randomUUID()))));

        mockMvc.perform(get("/vehicles")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listVehicles_withFilters() throws Exception {
        when(vehicleService.list(eq("Tesla"), eq(VehicleStatus.AVAILABLE),
                any(), any(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/vehicles")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .param("model", "Tesla").param("status", "AVAILABLE")
                        .param("priceMin", "10000").param("priceMax", "80000"))
                .andExpect(status().isOk());
    }

    @Test
    void listVehicles_premiumSubscription() throws Exception {
        when(vehicleService.list(isNull(), isNull(), isNull(), isNull(),
                eq(SubscriptionType.PREMIUM), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/vehicles")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .param("subscription", "PREMIUM"))
                .andExpect(status().isOk());
    }

    @Test
    void updateVehicle_success() throws Exception {
        UUID id = UUID.randomUUID();
        VehicleResponse updated = sampleResponse(id);
        updated.setStatus(VehicleStatus.SOLD);
        when(vehicleService.update(any(UUID.class), any())).thenReturn(updated);

        mockMvc.perform(patch("/vehicles/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "SOLD" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"));
    }

    @Test
    void deleteVehicle_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/vehicles/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isNoContent());

        verify(vehicleService).delete(id);
    }
}
