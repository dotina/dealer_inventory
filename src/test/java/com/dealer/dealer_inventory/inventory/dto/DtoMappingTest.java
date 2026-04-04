package com.dealer.dealer_inventory.inventory.dto;

import com.dealer.dealer_inventory.inventory.entity.Dealer;
import com.dealer.dealer_inventory.inventory.entity.Vehicle;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoMappingTest {

    /* ── DealerResponse.from ── */

    @Test
    void dealerResponse_from_mapsAllFields() {
        Dealer d = new Dealer();
        d.setId(UUID.randomUUID());
        d.setTenantId("t1");
        d.setName("Dealer A");
        d.setEmail("a@test.com");
        d.setSubscriptionType(SubscriptionType.PREMIUM);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());

        DealerResponse resp = DealerResponse.from(d);

        assertEquals(d.getId(), resp.getId());
        assertEquals("t1", resp.getTenantId());
        assertEquals("Dealer A", resp.getName());
        assertEquals("a@test.com", resp.getEmail());
        assertEquals(SubscriptionType.PREMIUM, resp.getSubscriptionType());
        assertNull(resp.getDeleted());  // not set in from()
        assertNull(resp.getDeletedAt());
    }

    @Test
    void dealerResponse_fromWithDeleteInfo_includesDeleteFields() {
        Dealer d = new Dealer();
        d.setId(UUID.randomUUID());
        d.setTenantId("t1");
        d.setName("Deleted");
        d.setEmail("del@test.com");
        d.setSubscriptionType(SubscriptionType.BASIC);
        d.setDeleted(true);
        d.setDeletedAt(Instant.now());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());

        DealerResponse resp = DealerResponse.fromWithDeleteInfo(d);

        assertTrue(resp.getDeleted());
        assertNotNull(resp.getDeletedAt());
    }

    /* ── VehicleResponse.from ── */

    @Test
    void vehicleResponse_from_mapsAllFields() {
        Dealer dealer = new Dealer();
        dealer.setId(UUID.randomUUID());

        Vehicle v = new Vehicle();
        v.setId(UUID.randomUUID());
        v.setTenantId("t1");
        v.setDealer(dealer);
        v.setModel("Model 3");
        v.setPrice(new BigDecimal("35000"));
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());

        VehicleResponse resp = VehicleResponse.from(v);

        assertEquals(v.getId(), resp.getId());
        assertEquals(dealer.getId(), resp.getDealerId());
        assertEquals("Model 3", resp.getModel());
        assertEquals(VehicleStatus.AVAILABLE, resp.getStatus());
        assertNull(resp.getDeleted());
    }

    @Test
    void vehicleResponse_fromWithDeleteInfo_includesDeleteFields() {
        Dealer dealer = new Dealer();
        dealer.setId(UUID.randomUUID());

        Vehicle v = new Vehicle();
        v.setId(UUID.randomUUID());
        v.setTenantId("t1");
        v.setDealer(dealer);
        v.setModel("X");
        v.setPrice(BigDecimal.TEN);
        v.setStatus(VehicleStatus.SOLD);
        v.setDeleted(true);
        v.setDeletedAt(Instant.now());
        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());

        VehicleResponse resp = VehicleResponse.fromWithDeleteInfo(v);

        assertTrue(resp.getDeleted());
        assertNotNull(resp.getDeletedAt());
    }

    @Test
    void vehicleResponse_from_nullDealer_usesDealerIdField() {
        Vehicle v = new Vehicle();
        v.setId(UUID.randomUUID());
        v.setTenantId("t1");
        v.setDealer(null);
        UUID dealerId = UUID.randomUUID();
        v.setDealerId(dealerId);
        v.setModel("Z");
        v.setPrice(BigDecimal.ONE);
        v.setStatus(VehicleStatus.AVAILABLE);
        v.setCreatedAt(Instant.now());
        v.setUpdatedAt(Instant.now());

        VehicleResponse resp = VehicleResponse.from(v);
        assertEquals(dealerId, resp.getDealerId());
    }
}

