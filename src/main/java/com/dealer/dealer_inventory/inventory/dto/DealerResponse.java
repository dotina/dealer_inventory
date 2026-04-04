package com.dealer.dealer_inventory.inventory.dto;

import com.dealer.dealer_inventory.inventory.entity.Dealer;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealerResponse {

    private UUID id;
    private String tenantId;
    private String name;
    private String email;
    private SubscriptionType subscriptionType;
    private Instant createdAt;
    private Instant updatedAt;

    // Soft-delete fields — only populated for admin queries
    private Boolean deleted;
    private Instant deletedAt;

    public static DealerResponse from(Dealer d) {
        return DealerResponse.builder()
                .id(d.getId())
                .tenantId(d.getTenantId())
                .name(d.getName())
                .email(d.getEmail())
                .subscriptionType(d.getSubscriptionType())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public static DealerResponse fromWithDeleteInfo(Dealer d) {
        return DealerResponse.builder()
                .id(d.getId())
                .tenantId(d.getTenantId())
                .name(d.getName())
                .email(d.getEmail())
                .subscriptionType(d.getSubscriptionType())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .deleted(d.isDeleted())
                .deletedAt(d.getDeletedAt())
                .build();
    }
}

