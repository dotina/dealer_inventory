package com.dealer.dealer_inventory.inventory.repository;

import com.dealer.dealer_inventory.inventory.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID>, JpaSpecificationExecutor<Vehicle> {

    /* ── tenant-scoped standard queries (respect @SQLRestriction) ── */

    Optional<Vehicle> findByIdAndTenantId(UUID id, String tenantId);

    Page<Vehicle> findAllByTenantId(String tenantId, Pageable pageable);

    List<Vehicle> findAllByDealerIdAndTenantId(UUID dealerId, String tenantId);

    /* ── subscription=PREMIUM filter: vehicles whose dealer has PREMIUM subscription, tenant-scoped ── */

    @Query("""
           SELECT v FROM Vehicle v JOIN v.dealer d
           WHERE v.tenantId = :tenantId
             AND d.subscriptionType = com.dealer.dealer_inventory.inventory.entity.enums.SubscriptionType.PREMIUM
           """)
    Page<Vehicle> findAllByPremiumDealerAndTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    /* ── cascade soft-delete for all vehicles of a dealer ── */

    @Modifying
    @Query("UPDATE Vehicle v SET v.deleted = true, v.deletedAt = :now, v.deletedBy = :by WHERE v.dealer.id = :dealerId AND v.tenantId = :tenantId AND v.deleted = false")
    int softDeleteAllByDealerId(@Param("dealerId") UUID dealerId,
                                @Param("tenantId") String tenantId,
                                @Param("now") Instant now,
                                @Param("by") String deletedBy);

    /* ── admin: include soft-deleted (native query bypasses @SQLRestriction) ── */

    @Query(value = "SELECT * FROM vehicles WHERE id = :id", nativeQuery = true)
    Optional<Vehicle> findByIdIncludingDeleted(@Param("id") UUID id);

    @Query(value = "SELECT * FROM vehicles ORDER BY created_at DESC",
           countQuery = "SELECT count(*) FROM vehicles",
           nativeQuery = true)
    Page<Vehicle> findAllIncludingDeleted(Pageable pageable);
}

