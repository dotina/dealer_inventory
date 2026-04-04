package com.dealer.dealer_inventory.inventory.dto;

import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VehicleUpdateRequest {

    private String model;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    private VehicleStatus status;
}

