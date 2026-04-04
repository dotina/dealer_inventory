package com.dealer.dealer_inventory.inventory.controller;

import com.dealer.dealer_inventory.audit.annotation.Audited;
import com.dealer.dealer_inventory.audit.entity.AuditAction;
import com.dealer.dealer_inventory.inventory.dto.DealerResponse;
import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints. All methods require the {@code GLOBAL_ADMIN} role.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Returns the count of dealers grouped by subscription type.
     * <p>
     * <b>Note:</b> This count is <em>overall (cross-tenant)</em> because the caller
     * is a GLOBAL_ADMIN with visibility across all tenants.
     * </p>
     * Expected response: {@code { "BASIC": n, "PREMIUM": n }}
     */
    @GetMapping("/dealers/countBySubscription")
    @Audited(action = AuditAction.ADMIN_QUERY, entity = "Dealer")
    public Map<String, Long> countBySubscription() {
        return adminService.countBySubscription();
    }

    /**
     * List all dealers. Pass {@code includeDeleted=true} to see soft-deleted records.
     */
    @GetMapping("/dealers")
    @Audited(action = AuditAction.ADMIN_QUERY, entity = "Dealer")
    public Page<DealerResponse> listDealers(
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable) {
        return adminService.listAllDealers(includeDeleted, pageable);
    }

    /**
     * Get a single dealer by ID. Pass {@code includeDeleted=true} to fetch even if soft-deleted.
     */
    @GetMapping("/dealers/{id}")
    @Audited(action = AuditAction.ADMIN_QUERY, entity = "Dealer")
    public DealerResponse getDealerById(@PathVariable UUID id,
                                         @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return adminService.getDealerById(id, includeDeleted);
    }

    /**
     * List all vehicles. Pass {@code includeDeleted=true} to see soft-deleted records.
     */
    @GetMapping("/vehicles")
    @Audited(action = AuditAction.ADMIN_QUERY, entity = "Vehicle")
    public Page<VehicleResponse> listVehicles(
            @RequestParam(defaultValue = "false") boolean includeDeleted,
            Pageable pageable) {
        return adminService.listAllVehicles(includeDeleted, pageable);
    }

    /**
     * Get a single vehicle by ID. Pass {@code includeDeleted=true} to fetch even if soft-deleted.
     */
    @GetMapping("/vehicles/{id}")
    @Audited(action = AuditAction.ADMIN_QUERY, entity = "Vehicle")
    public VehicleResponse getVehicleById(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "false") boolean includeDeleted) {
        return adminService.getVehicleById(id, includeDeleted);
    }
}

