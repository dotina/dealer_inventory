package com.dealer.dealer_inventory.inventory.dto;

import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class DealerUpdateRequest {

    private String name;

    @Email(message = "Email must be valid")
    private String email;

    private SubscriptionType subscriptionType;
}

