package com.dealer.dealer_inventory.inventory.controller;

import com.dealer.dealer_inventory.inventory.dto.VehicleCreateRequest;
import com.dealer.dealer_inventory.inventory.dto.VehicleResponse;
import com.dealer.dealer_inventory.inventory.dto.VehicleUpdateRequest;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import com.dealer.dealer_inventory.inventory.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleCreateRequest request) {
        return vehicleService.create(request);
    }

    @GetMapping("/{id}")
    public VehicleResponse getById(@PathVariable UUID id) {
        return vehicleService.getById(id);
    }

    /**
     * List vehicles with optional filters.
     * <ul>
     *   <li><b>model</b> — partial match (case-insensitive)</li>
     *   <li><b>status</b> — AVAILABLE | SOLD</li>
     *   <li><b>priceMin / priceMax</b> — inclusive range</li>
     *   <li><b>subscription</b> — PREMIUM returns only vehicles whose dealer is PREMIUM, tenant-scoped</li>
     * </ul>
     * Supports pagination &amp; sort via standard Spring {@code Pageable} params
     * (e.g. {@code ?page=0&size=10&sort=price,asc}).
     */
    @GetMapping
    public Page<VehicleResponse> list(@RequestParam(required = false) String model,
                                       @RequestParam(required = false) VehicleStatus status,
                                       @RequestParam(required = false) BigDecimal priceMin,
                                       @RequestParam(required = false) BigDecimal priceMax,
                                       @RequestParam(required = false) SubscriptionType subscription,
                                       Pageable pageable) {
        return vehicleService.list(model, status, priceMin, priceMax, subscription, pageable);
    }

    @PatchMapping("/{id}")
    public VehicleResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody VehicleUpdateRequest request) {
        return vehicleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        vehicleService.delete(id);
    }
}

