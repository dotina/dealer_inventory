package com.dealer.dealer_inventory.inventory.service;

import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.inventory.dto.VehicleCreateRequest;
import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.dto.VehicleUpdateRequest;
import com.dealer.dealer_inventory.inventory.entity.Dealer;
import com.dealer.dealer_inventory.inventory.entity.Vehicle;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DealerService dealerService;
    @InjectMocks private VehicleService vehicleService;

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
        d.setName("Dealer");
        d.setEmail("d@test.com");
        d.setSubscriptionType(SubscriptionType.BASIC);
        return d;
    }

    private Vehicle buildVehicle(UUID id, Dealer dealer) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setTenantId(TENANT);
        v.setDealer(dealer);
        v.setModel("Model S");
        v.setPrice(new BigDecimal("50000.00"));
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());
        return v;
    }

    /* ── CREATE ── */

    @Test
    void create_savesVehicleWithCorrectTenant() {
        UUID dealerId = UUID.randomUUID();
        Dealer dealer = buildDealer(dealerId);
        when(dealerService.findEntityByIdAndTenant(dealerId, TENANT)).thenReturn(dealer);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> {
            Vehicle v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            v.setCreatedAt(Instant.now());
            v.setUpdatedAt(Instant.now());
            return v;
        });

        VehicleCreateRequest req = new VehicleCreateRequest();
        req.setDealerId(dealerId);
        req.setModel("Model 3");
        req.setPrice(new BigDecimal("35000"));
        req.setStatus(VehicleStatus.AVAILABLE);

        VehicleResponse resp = vehicleService.create(req);

        assertNotNull(resp.getId());
        assertEquals("Model 3", resp.getModel());
        assertEquals(TENANT, resp.getTenantId());
    }

    @Test
    void create_dealerNotFound_throws() {
        UUID dealerId = UUID.randomUUID();
        when(dealerService.findEntityByIdAndTenant(dealerId, TENANT))
                .thenThrow(new ResourceNotFoundException("Dealer", dealerId));

        VehicleCreateRequest req = new VehicleCreateRequest();
        req.setDealerId(dealerId);
        req.setModel("X");
        req.setPrice(BigDecimal.TEN);
        req.setStatus(VehicleStatus.AVAILABLE);

        assertThrows(ResourceNotFoundException.class, () -> vehicleService.create(req));
    }

    /* ── GET BY ID ── */

    @Test
    void getById_found_returnsResponse() {
        UUID vid = UUID.randomUUID();
        Vehicle v = buildVehicle(vid, buildDealer(UUID.randomUUID()));
        when(vehicleRepository.findByIdAndTenantId(vid, TENANT)).thenReturn(Optional.of(v));

        VehicleResponse resp = vehicleService.getById(vid);
        assertEquals(vid, resp.getId());
        assertEquals("Model S", resp.getModel());
    }

    @Test
    void getById_notFound_throws() {
        UUID vid = UUID.randomUUID();
        when(vehicleRepository.findByIdAndTenantId(vid, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> vehicleService.getById(vid));
    }

    /* ── LIST ── */

    @Test
    void list_noFilters_returnsTenantScoped() {
        Pageable pageable = PageRequest.of(0, 10);
        Vehicle v = buildVehicle(UUID.randomUUID(), buildDealer(UUID.randomUUID()));
        Page<Vehicle> page = new PageImpl<>(List.of(v), pageable, 1);
        when(vehicleRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<VehicleResponse> result = vehicleService.list(null, null, null, null, null, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void list_withPremiumSubscription_usesSpecificationPath() {
        Pageable pageable = PageRequest.of(0, 10);
        Vehicle v = buildVehicle(UUID.randomUUID(), buildDealer(UUID.randomUUID()));
        Page<Vehicle> page = new PageImpl<>(List.of(v), pageable, 1);
        when(vehicleRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<VehicleResponse> result = vehicleService.list(
                "Model", VehicleStatus.AVAILABLE, null, null, SubscriptionType.PREMIUM, pageable);

        assertEquals(1, result.getTotalElements());
        verify(vehicleRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void list_invalidPriceRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> vehicleService.list(
                null, null, new BigDecimal("50000"), new BigDecimal("10000"), null, PageRequest.of(0, 10)));
    }

    @Test
    void list_withAllFilters_appliesSpecifications() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehicle> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(vehicleRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        vehicleService.list("Tesla", VehicleStatus.AVAILABLE,
                new BigDecimal("10000"), new BigDecimal("80000"),
                SubscriptionType.BASIC, pageable);

        verify(vehicleRepository).findAll(any(Specification.class), eq(pageable));
    }

    /* ── UPDATE ── */

    @Test
    void update_patchesOnlyProvidedFields() {
        UUID vid = UUID.randomUUID();
        Vehicle existing = buildVehicle(vid, buildDealer(UUID.randomUUID()));
        when(vehicleRepository.findByIdAndTenantId(vid, TENANT)).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleUpdateRequest req = new VehicleUpdateRequest();
        req.setStatus(VehicleStatus.SOLD);

        VehicleResponse resp = vehicleService.update(vid, req);

        assertEquals(VehicleStatus.SOLD, resp.getStatus());
        assertEquals("Model S", resp.getModel()); // unchanged
        assertEquals(new BigDecimal("50000.00"), resp.getPrice()); // unchanged
    }

    @Test
    void update_notFound_throws() {
        UUID vid = UUID.randomUUID();
        when(vehicleRepository.findByIdAndTenantId(vid, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> vehicleService.update(vid, new VehicleUpdateRequest()));
    }

    /* ── DELETE (soft) ── */

    @Test
    void delete_softDeletesVehicle() {
        UUID vid = UUID.randomUUID();
        Vehicle v = buildVehicle(vid, buildDealer(UUID.randomUUID()));
        when(vehicleRepository.findByIdAndTenantId(vid, TENANT)).thenReturn(Optional.of(v));
        when(vehicleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        vehicleService.delete(vid);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertTrue(captor.getValue().isDeleted());
        assertNotNull(captor.getValue().getDeletedAt());
        assertEquals(TENANT, captor.getValue().getDeletedBy());
    }

    @Test
    void delete_notFound_throws() {
        UUID vid = UUID.randomUUID();
        when(vehicleRepository.findByIdAndTenantId(vid, TENANT)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> vehicleService.delete(vid));
    }
}

