package com.dealer.dealer_inventory.inventory.service;

import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.inventory.dto.DealerCreateRequest;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.DealerUpdateRequest;
import com.dealer.dealer_inventory.inventory.entity.Dealer;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.repository.DealerRepository;
import com.dealer.dealer_inventory.inventory.repository.VehicleRepository;
import com.dealer.dealer_inventory.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DealerServiceTest {

    @Mock private DealerRepository dealerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @InjectMocks private DealerService dealerService;

    private static final String TENANT = "test-tenant";

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Dealer buildDealer(UUID id) {
        Dealer d = new Dealer();
        d.setId(id);
        d.setTenantId(TENANT);
        d.setName("Test Dealer");
        d.setEmail("test@example.com");
        d.setSubscriptionType(SubscriptionType.BASIC);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }

    /* ── CREATE ── */

    @Test
    void create_savesAndReturnsDealerResponse() {
        DealerCreateRequest req = new DealerCreateRequest();
        req.setName("New Dealer");
        req.setEmail("new@example.com");
        req.setSubscriptionType(SubscriptionType.PREMIUM);

        when(dealerRepository.save(any(Dealer.class))).thenAnswer(inv -> {
            Dealer d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            d.setCreatedAt(Instant.now());
            d.setUpdatedAt(Instant.now());
            return d;
        });

        DealerResponse resp = dealerService.create(req);

        assertNotNull(resp.getId());
        assertEquals("New Dealer", resp.getName());
        assertEquals("new@example.com", resp.getEmail());
        assertEquals(SubscriptionType.PREMIUM, resp.getSubscriptionType());
        assertEquals(TENANT, resp.getTenantId());

        ArgumentCaptor<Dealer> captor = ArgumentCaptor.forClass(Dealer.class);
        verify(dealerRepository).save(captor.capture());
        assertEquals(TENANT, captor.getValue().getTenantId());
    }

    /* ── GET BY ID ── */

    @Test
    void getById_found_returnsResponse() {
        UUID id = UUID.randomUUID();
        Dealer dealer = buildDealer(id);
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(dealer));

        DealerResponse resp = dealerService.getById(id);

        assertEquals(id, resp.getId());
        assertEquals("Test Dealer", resp.getName());
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> dealerService.getById(id));
    }

    /* ── LIST ── */

    @Test
    void list_returnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Dealer d = buildDealer(UUID.randomUUID());
        Page<Dealer> page = new PageImpl<>(List.of(d), pageable, 1);
        when(dealerRepository.findAllByTenantId(TENANT, pageable)).thenReturn(page);

        Page<DealerResponse> result = dealerService.list(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Dealer", result.getContent().get(0).getName());
    }

    /* ── UPDATE ── */

    @Test
    void update_patchesFieldsAndReturnsUpdated() {
        UUID id = UUID.randomUUID();
        Dealer existing = buildDealer(id);
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(existing));
        when(dealerRepository.save(any(Dealer.class))).thenAnswer(inv -> inv.getArgument(0));

        DealerUpdateRequest req = new DealerUpdateRequest();
        req.setName("Updated Name");
        req.setSubscriptionType(SubscriptionType.PREMIUM);

        DealerResponse resp = dealerService.update(id, req);

        assertEquals("Updated Name", resp.getName());
        assertEquals(SubscriptionType.PREMIUM, resp.getSubscriptionType());
        assertEquals("test@example.com", resp.getEmail()); // unchanged
    }

    @Test
    void update_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> dealerService.update(id, new DealerUpdateRequest()));
    }

    @Test
    void update_nullFields_doesNotOverwrite() {
        UUID id = UUID.randomUUID();
        Dealer existing = buildDealer(id);
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(existing));
        when(dealerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DealerUpdateRequest req = new DealerUpdateRequest(); // all null

        DealerResponse resp = dealerService.update(id, req);
        assertEquals("Test Dealer", resp.getName());
        assertEquals("test@example.com", resp.getEmail());
        assertEquals(SubscriptionType.BASIC, resp.getSubscriptionType());
    }

    /* ── DELETE (soft) ── */

    @Test
    void delete_softDeletesDealerAndCascadesToVehicles() {
        UUID id = UUID.randomUUID();
        Dealer dealer = buildDealer(id);
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(dealer));
        when(vehicleRepository.softDeleteAllByDealerId(eq(id), eq(TENANT), any(Instant.class), eq(TENANT)))
                .thenReturn(3);
        when(dealerRepository.save(any(Dealer.class))).thenAnswer(inv -> inv.getArgument(0));

        dealerService.delete(id);

        verify(vehicleRepository).softDeleteAllByDealerId(eq(id), eq(TENANT), any(), eq(TENANT));
        ArgumentCaptor<Dealer> captor = ArgumentCaptor.forClass(Dealer.class);
        verify(dealerRepository).save(captor.capture());
        assertTrue(captor.getValue().isDeleted());
        assertNotNull(captor.getValue().getDeletedAt());
    }

    @Test
    void delete_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> dealerService.delete(id));
    }

    /* ── findEntityByIdAndTenant ── */

    @Test
    void findEntityByIdAndTenant_found_returnsEntity() {
        UUID id = UUID.randomUUID();
        Dealer dealer = buildDealer(id);
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(dealer));

        Dealer result = dealerService.findEntityByIdAndTenant(id, TENANT);
        assertEquals(id, result.getId());
    }

    @Test
    void findEntityByIdAndTenant_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> dealerService.findEntityByIdAndTenant(id, TENANT));
    }
}

