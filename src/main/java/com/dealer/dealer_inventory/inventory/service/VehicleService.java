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
import com.dealer.dealer_inventory.inventory.repository.VehicleSpecification;
import com.dealer.dealer_inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DealerService dealerService;

    /* ── CREATE ────────────────────────────────────────── */

    @Transactional
    public VehicleResponse create(VehicleCreateRequest req) {
        String tenantId = TenantContext.require();

        // Verify the dealer belongs to the same tenant
        Dealer dealer = dealerService.findEntityByIdAndTenant(req.getDealerId(), tenantId);

        Vehicle vehicle = new Vehicle();
        vehicle.setTenantId(tenantId);
        vehicle.setDealer(dealer);
        vehicle.setModel(req.getModel());
        vehicle.setPrice(req.getPrice());
        vehicle.setStatus(req.getStatus());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    /* ── READ (single) ────────────────────────────────── */

    public VehicleResponse getById(UUID id) {
        String tenantId = TenantContext.require();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        return VehicleResponse.from(vehicle);
    }

    /* ── LIST (filtered, paginated, tenant-scoped) ────── */

    public Page<VehicleResponse> list(String model,
                                       VehicleStatus status,
                                       BigDecimal priceMin,
                                       BigDecimal priceMax,
                                       SubscriptionType subscription,
                                       Pageable pageable) {
        String tenantId = TenantContext.require();

        // subscription=PREMIUM shortcut uses dedicated JPQL query
        if (subscription == SubscriptionType.PREMIUM) {
            return vehicleRepository.findAllByPremiumDealerAndTenantId(tenantId, pageable)
                    .map(VehicleResponse::from);
        }

        Specification<Vehicle> spec = Specification.where(VehicleSpecification.hasTenant(tenantId));

        if (model != null && !model.isBlank()) {
            spec = spec.and(VehicleSpecification.hasModel(model));
        }
        if (status != null) {
            spec = spec.and(VehicleSpecification.hasStatus(status));
        }
        if (priceMin != null) {
            spec = spec.and(VehicleSpecification.hasPriceMin(priceMin));
        }
        if (priceMax != null) {
            spec = spec.and(VehicleSpecification.hasPriceMax(priceMax));
        }
        if (subscription != null) {
            spec = spec.and(VehicleSpecification.hasDealerSubscription(subscription));
        }

        return vehicleRepository.findAll(spec, pageable).map(VehicleResponse::from);
    }

    /* ── UPDATE (PATCH) ───────────────────────────────── */

    @Transactional
    public VehicleResponse update(UUID id, VehicleUpdateRequest req) {
        String tenantId = TenantContext.require();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        if (req.getModel() != null) vehicle.setModel(req.getModel());
        if (req.getPrice() != null) vehicle.setPrice(req.getPrice());
        if (req.getStatus() != null) vehicle.setStatus(req.getStatus());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    /* ── DELETE (soft) ────────────────────────────────── */

    @Transactional
    public void delete(UUID id) {
        String tenantId = TenantContext.require();
        Vehicle vehicle = vehicleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        vehicle.setDeleted(true);
        vehicle.setDeletedAt(Instant.now());
        vehicle.setDeletedBy(tenantId);
        vehicleRepository.save(vehicle);
    }
}

