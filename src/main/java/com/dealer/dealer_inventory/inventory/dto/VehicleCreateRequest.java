package com.dealer.dealer_inventory.inventory.dto;

import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VehicleCreateRequest {

    @NotNull(message = "Dealer ID is required")
    private UUID dealerId;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Status is required")
    private VehicleStatus status;
}

