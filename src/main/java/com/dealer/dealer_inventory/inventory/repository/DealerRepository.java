package com.dealer.dealer_inventory.inventory.repository;

import com.dealer.dealer_inventory.inventory.entity.Dealer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, UUID>, JpaSpecificationExecutor<Dealer> {

    /* ── tenant-scoped standard queries (respect @SQLRestriction) ── */

    Optional<Dealer> findByIdAndTenantId(UUID id, String tenantId);

    Page<Dealer> findAllByTenantId(String tenantId, Pageable pageable);

    List<Dealer> findAllByTenantIdAndIdIn(String tenantId, List<UUID> ids);

    /* ── admin: count by subscription (cross-tenant, non-deleted only) ── */

    @Query("SELECT d.subscriptionType, COUNT(d) FROM Dealer d GROUP BY d.subscriptionType")
    List<Object[]> countGroupBySubscriptionType();

    /* ── admin: include soft-deleted records (native query bypasses @SQLRestriction) ── */

    @Query(value = "SELECT * FROM dealers WHERE id = :id", nativeQuery = true)
    Optional<Dealer> findByIdIncludingDeleted(@Param("id") UUID id);

    @Query(value = "SELECT * FROM dealers WHERE tenant_id = :tenantId ORDER BY created_at DESC",
           countQuery = "SELECT count(*) FROM dealers WHERE tenant_id = :tenantId",
           nativeQuery = true)
    Page<Dealer> findAllByTenantIdIncludingDeleted(@Param("tenantId") String tenantId, Pageable pageable);

    @Query(value = "SELECT * FROM dealers ORDER BY created_at DESC",
           countQuery = "SELECT count(*) FROM dealers",
           nativeQuery = true)
    Page<Dealer> findAllIncludingDeleted(Pageable pageable);
}

