package com.dealer.dealer_inventory.inventory.service;

import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.entity.Dealer;
import com.dealer.dealer_inventory.inventory.entity.Vehicle;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import com.dealer.dealer_inventory.inventory.repository.DealerRepository;
import com.dealer.dealer_inventory.inventory.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private DealerRepository dealerRepository;
    @Mock private VehicleRepository vehicleRepository;
    @InjectMocks private AdminService adminService;

    private Dealer buildDealer(UUID id, boolean deleted) {
        Dealer d = new Dealer();
        d.setId(id);
        d.setTenantId("t1");
        d.setName("Dealer " + id);
        d.setEmail("d@test.com");
        d.setSubscriptionType(SubscriptionType.BASIC);
        d.setDeleted(deleted);
        d.setDeletedAt(deleted ? Instant.now() : null);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return d;
    }

    private Vehicle buildVehicle(UUID id, boolean deleted) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setTenantId("t1");
        v.setDealer(buildDealer(UUID.randomUUID(), false));
        v.setModel("X");
        v.setPrice(BigDecimal.TEN);
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setDeleted(deleted);
        v.setDeletedAt(deleted ? Instant.now() : null);
        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());
        return v;
    }

    /* ── countBySubscription ── */

    @Test
    void countBySubscription_returnsGroupedCounts() {
        when(dealerRepository.countGroupBySubscriptionType()).thenReturn(List.of(
                new Object[]{SubscriptionType.BASIC, 5L},
                new Object[]{SubscriptionType.PREMIUM, 3L}
        ));

        Map<String, Long> result = adminService.countBySubscription();

        assertEquals(5L, result.get("BASIC"));
        assertEquals(3L, result.get("PREMIUM"));
    }

    @Test
    void countBySubscription_emptyDatabase_returnsZeros() {
        when(dealerRepository.countGroupBySubscriptionType()).thenReturn(List.of());

        Map<String, Long> result = adminService.countBySubscription();

        assertEquals(0L, result.get("BASIC"));
        assertEquals(0L, result.get("PREMIUM"));
    }

    /* ── listAllDealers ── */

    @Test
    void listAllDealers_includingDeleted_usesNativeQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Dealer d = buildDealer(UUID.randomUUID(), true);
        Page<Dealer> page = new PageImpl<>(List.of(d), pageable, 1);
        when(dealerRepository.findAllIncludingDeleted(pageable)).thenReturn(page);

        Page<DealerResponse> result = adminService.listAllDealers(true, pageable);

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getDeleted());
        verify(dealerRepository).findAllIncludingDeleted(pageable);
    }

    @Test
    void listAllDealers_excludingDeleted_usesStandardQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(dealerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        adminService.listAllDealers(false, pageable);

        verify(dealerRepository).findAll(pageable);
        verify(dealerRepository, never()).findAllIncludingDeleted(any());
    }

    /* ── getDealerById ── */

    @Test
    void getDealerById_includingDeleted_usesNativeQuery() {
        UUID id = UUID.randomUUID();
        Dealer d = buildDealer(id, true);
        when(dealerRepository.findByIdIncludingDeleted(id)).thenReturn(Optional.of(d));

        DealerResponse resp = adminService.getDealerById(id, true);

        assertEquals(id, resp.getId());
        assertTrue(resp.getDeleted());
    }

    @Test
    void getDealerById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(dealerRepository.findByIdIncludingDeleted(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.getDealerById(id, true));
    }

    @Test
    void getDealerById_excludingDeleted_usesStandardQuery() {
        UUID id = UUID.randomUUID();
        Dealer d = buildDealer(id, false);
        when(dealerRepository.findById(id)).thenReturn(Optional.of(d));

        DealerResponse resp = adminService.getDealerById(id, false);
        assertEquals(id, resp.getId());
    }

    /* ── listAllVehicles ── */

    @Test
    void listAllVehicles_includingDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        Vehicle v = buildVehicle(UUID.randomUUID(), true);
        when(vehicleRepository.findAllIncludingDeleted(pageable))
                .thenReturn(new PageImpl<>(List.of(v), pageable, 1));

        Page<VehicleResponse> result = adminService.listAllVehicles(true, pageable);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getDeleted());
    }

    @Test
    void listAllVehicles_excludingDeleted() {
        Pageable pageable = PageRequest.of(0, 10);
        when(vehicleRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        adminService.listAllVehicles(false, pageable);
        verify(vehicleRepository).findAll(pageable);
    }

    /* ── getVehicleById ── */

    @Test
    void getVehicleById_includingDeleted() {
        UUID id = UUID.randomUUID();
        Vehicle v = buildVehicle(id, true);
        when(vehicleRepository.findByIdIncludingDeleted(id)).thenReturn(Optional.of(v));

        VehicleResponse resp = adminService.getVehicleById(id, true);
        assertEquals(id, resp.getId());
        assertTrue(resp.getDeleted());
    }

    @Test
    void getVehicleById_excludingDeleted() {
        UUID id = UUID.randomUUID();
        Vehicle v = buildVehicle(id, false);
        when(vehicleRepository.findById(id)).thenReturn(Optional.of(v));

        VehicleResponse resp = adminService.getVehicleById(id, false);
        assertEquals(id, resp.getId());
    }

    @Test
    void getVehicleById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(vehicleRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.getVehicleById(id, false));
    }
}

