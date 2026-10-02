package com.zonapos.repository;

import com.zonapos.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<PurchaseOrder> findByTenantIdAndProductIdOrderByCreatedAtDesc(Long tenantId, Long productId);
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId AND po.productId = :productId AND (:variantId IS NULL OR po.variantId = :variantId) ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findHistoryByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId, @Param("variantId") Long variantId);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId AND po.productId = :productId AND (:variantId IS NULL OR po.variantId = :variantId) ORDER BY po.createdAt DESC LIMIT 1")
    Optional<PurchaseOrder> findLastPurchase(@Param("tenantId") Long tenantId, @Param("productId") Long productId, @Param("variantId") Long variantId);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.tenantId = :tenantId " +
           "AND (:supplierId IS NULL OR po.supplierId = :supplierId) " +
           "AND (:storageId IS NULL OR po.storageId = :storageId) " +
           "AND (:outletId IS NULL OR po.outletId = :outletId) " +
           "AND (cast(:startDate as timestamp) IS NULL OR po.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR po.createdAt <= :endDate) " +
           "ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findPurchaseOrdersFiltered(
            @Param("tenantId") Long tenantId,
            @Param("supplierId") Long supplierId,
            @Param("storageId") Long storageId,
            @Param("outletId") Long outletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
