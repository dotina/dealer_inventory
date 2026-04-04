package com.dealer.dealer_inventory.inventory.service;

import com.dealer.dealer_inventory.exception.ResourceNotFoundException;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.repository.DealerRepository;
import com.dealer.dealer_inventory.inventory.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for GLOBAL_ADMIN-only operations.
 * <p>
 * <b>countBySubscription</b> returns overall (cross-tenant) counts because the
 * caller is a GLOBAL_ADMIN who has visibility across all tenants.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final DealerRepository dealerRepository;
    private final VehicleRepository vehicleRepository;

    /* ── Count dealers grouped by subscription (OVERALL, cross-tenant) ── */

    public Map<String, Long> countBySubscription() {
        List<Object[]> rows = dealerRepository.countGroupBySubscriptionType();
        Map<String, Long> result = new LinkedHashMap<>();
        // Initialise both keys so the response always contains them
        result.put(SubscriptionType.BASIC.name(), 0L);
        result.put(SubscriptionType.PREMIUM.name(), 0L);
        for (Object[] row : rows) {
            result.put(((SubscriptionType) row[0]).name(), (Long) row[1]);
        }
        return result;
    }

    /* ── Dealers including soft-deleted (admin visibility) ── */

    public Page<DealerResponse> listAllDealers(boolean includeDeleted, Pageable pageable) {
        if (includeDeleted) {
            return dealerRepository.findAllIncludingDeleted(pageable)
                    .map(DealerResponse::fromWithDeleteInfo);
        }
        return dealerRepository.findAll(pageable).map(DealerResponse::from);
    }

    public DealerResponse getDealerById(UUID id, boolean includeDeleted) {
        if (includeDeleted) {
            return dealerRepository.findByIdIncludingDeleted(id)
                    .map(DealerResponse::fromWithDeleteInfo)
                    .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));
        }
        return dealerRepository.findById(id)
                .map(DealerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer", id));
    }

    /* ── Vehicles including soft-deleted (admin visibility) ── */

    public Page<VehicleResponse> listAllVehicles(boolean includeDeleted, Pageable pageable) {
        if (includeDeleted) {
            return vehicleRepository.findAllIncludingDeleted(pageable)
                    .map(VehicleResponse::fromWithDeleteInfo);
        }
        return vehicleRepository.findAll(pageable).map(VehicleResponse::from);
    }

    public VehicleResponse getVehicleById(UUID id, boolean includeDeleted) {
        if (includeDeleted) {
            return vehicleRepository.findByIdIncludingDeleted(id)
                    .map(VehicleResponse::fromWithDeleteInfo)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        }
        return vehicleRepository.findById(id)
                .map(VehicleResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }
}

