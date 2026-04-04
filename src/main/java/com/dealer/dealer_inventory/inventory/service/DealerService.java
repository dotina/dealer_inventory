package com.dealer.dealer_inventory.inventory.service;

import com.dealer.dealer_inventory.exception.ForbiddenException;
import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.inventory.dto.DealerCreateRequest;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.DealerUpdateRequest;
import com.dealer.dealer_inventory.inventory.entity.Dealer;
import com.dealer.dealer_inventory.inventory.repository.DealerRepository;
import com.dealer.dealer_inventory.inventory.repository.VehicleRepository;
import com.dealer.dealer_inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealerService {

    private final DealerRepository dealerRepository;
    private final VehicleRepository vehicleRepository;

    /* ── CREATE ────────────────────────────────────────── */

    @Transactional
    public DealerResponse create(DealerCreateRequest req) {
        String tenantId = TenantContext.require();

        Dealer dealer = new Dealer();
        dealer.setTenantId(tenantId);
        dealer.setName(req.getName());
        dealer.setEmail(req.getEmail());
        dealer.setSubscriptionType(req.getSubscriptionType());

        return DealerResponse.from(dealerRepository.save(dealer));
    }

    /* ── READ (single) ────────────────────────────────── */

    public DealerResponse getById(UUID id) {
        String tenantId = TenantContext.require();
        Dealer dealer = dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));
        return DealerResponse.from(dealer);
    }

    /* ── LIST (paginated/sorted, tenant-scoped) ───────── */

    public Page<DealerResponse> list(Pageable pageable) {
        String tenantId = TenantContext.require();
        return dealerRepository.findAllByTenantId(tenantId, pageable)
                .map(DealerResponse::from);
    }

    /* ── UPDATE (PATCH) ───────────────────────────────── */

    @Transactional
    public DealerResponse update(UUID id, DealerUpdateRequest req) {
        String tenantId = TenantContext.require();
        Dealer dealer = dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));

        if (req.getName() != null) dealer.setName(req.getName());
        if (req.getEmail() != null) dealer.setEmail(req.getEmail());
        if (req.getSubscriptionType() != null) dealer.setSubscriptionType(req.getSubscriptionType());

        return DealerResponse.from(dealerRepository.save(dealer));
    }

    /* ── DELETE (soft) ────────────────────────────────── */

    @Transactional
    public void delete(UUID id) {
        String tenantId = TenantContext.require();
        Dealer dealer = dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));

        // Cascade soft-delete to all dealer's vehicles
        vehicleRepository.softDeleteAllByDealerId(dealer.getId(), tenantId, Instant.now(), tenantId);

        dealer.setDeleted(true);
        dealer.setDeletedAt(Instant.now());
        dealer.setDeletedBy(tenantId);
        dealerRepository.save(dealer);
    }

    /* ── Internal helper for vehicle service ───────────── */

    public Dealer findEntityByIdAndTenant(UUID id, String tenantId) {
        return dealerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));
    }
}

