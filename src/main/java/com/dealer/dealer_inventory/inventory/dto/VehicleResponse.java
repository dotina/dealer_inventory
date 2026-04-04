package com.dealer.dealer_inventory.inventory.dto;

import com.dealer.dealer_inventory.inventory.entity.Vehicle;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VehicleResponse {

    private UUID id;
    private String tenantId;
    private UUID dealerId;
    private String model;
    private BigDecimal price;
    private VehicleStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    // Soft-delete fields — only populated for admin queries
    private Boolean deleted;
    private Instant deletedAt;

    public static VehicleResponse from(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .tenantId(v.getTenantId())
                .dealerId(v.getDealer() != null ? v.getDealer().getId() : v.getDealerId())
                .model(v.getModel())
                .price(v.getPrice())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    public static VehicleResponse fromWithDeleteInfo(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .tenantId(v.getTenantId())
                .dealerId(v.getDealer() != null ? v.getDealer().getId() : v.getDealerId())
                .model(v.getModel())
                .price(v.getPrice())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .deleted(v.isDeleted())
                .deletedAt(v.getDeletedAt())
                .build();
    }
}

