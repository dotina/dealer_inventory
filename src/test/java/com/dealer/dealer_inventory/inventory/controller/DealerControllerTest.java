package com.dealer.dealer_inventory.inventory.controller;

import com.dealer.dealer_inventory.exception.GlobalExceptionHandler;
import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.service.DealerService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class DealerControllerTest {

    @Autowired private WebApplicationContext wac;
    @MockitoBean private DealerService dealerService;
    private MockMvc mockMvc;

    private static final String TENANT = "test-tenant";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    private DealerResponse sampleResponse(UUID id) {
        return DealerResponse.builder()
                .id(id).tenantId(TENANT).name("Test Dealer")
                .email("test@example.com").subscriptionType(SubscriptionType.BASIC)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    @Test
    void missingTenantId_returns400() throws Exception {
        mockMvc.perform(get("/dealers"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDealer_success() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealerService.create(any())).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/dealers")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Test Dealer", "email": "test@example.com", "subscriptionType": "BASIC" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Test Dealer"));
    }

    @Test
    void createDealer_validationError_missingName() throws Exception {
        mockMvc.perform(post("/dealers")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "test@example.com", "subscriptionType": "BASIC" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDealer_validationError_invalidEmail() throws Exception {
        mockMvc.perform(post("/dealers")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Dealer", "email": "not-an-email", "subscriptionType": "BASIC" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDealerById_success() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealerService.getById(id)).thenReturn(sampleResponse(id));

        mockMvc.perform(get("/dealers/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getDealerById_notFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealerService.getById(id)).thenThrow(new ResourceNotFoundException("Dealer", id));

        mockMvc.perform(get("/dealers/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listDealers_success() throws Exception {
        UUID id = UUID.randomUUID();
        when(dealerService.list(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(id))));

        mockMvc.perform(get("/dealers")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Test Dealer"));
    }

    @Test
    void updateDealer_success() throws Exception {
        UUID id = UUID.randomUUID();
        DealerResponse updated = sampleResponse(id);
        updated.setName("Updated");
        when(dealerService.update(any(UUID.class), any())).thenReturn(updated);

        mockMvc.perform(patch("/dealers/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Updated" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteDealer_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/dealers/{id}", id)
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isNoContent());

        verify(dealerService).delete(id);
    }

    @Test
    void adminEndpoint_withUserRole_returns403() throws Exception {
        mockMvc.perform(get("/admin/dealers/countBySubscription")
                        .header("X-Tenant-Id", TENANT).header("X-Role", "USER"))
                .andExpect(status().isForbidden());
    }
}
