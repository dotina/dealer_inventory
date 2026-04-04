package com.dealer.dealer_inventory.inventory.repository;

import com.dealer.dealer_inventory.inventory.entity.Vehicle;
import com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType;
import com.dealer.dealer_inventory.inventory.entity.enums.VehicleStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/**
 * Dynamic {@link Specification} builders for {@link Vehicle} filtering.
 */
public final class VehicleSpecification {

    private VehicleSpecification() { }

    public static Specification<Vehicle> hasTenant(String tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Vehicle> hasModel(String model) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("model")),
                "%" + model.toLowerCase() + "%");
    }

    public static Specification<Vehicle> hasStatus(VehicleStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Vehicle> hasPriceMin(BigDecimal min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Vehicle> hasPriceMax(BigDecimal max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Vehicle> hasDealerSubscription(SubscriptionType type) {
        return (root, query, cb) -> cb.equal(root.get("dealer").get("subscriptionType"), type);
    }
}

