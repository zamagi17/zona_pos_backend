package com.zonapos.repository;

import com.zonapos.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductIdAndVariantIdAndOutletId(Long productId, Long variantId, Long outletId);
    Optional<Stock> findByProductIdAndVariantIdIsNullAndOutletId(Long productId, Long outletId);
    Optional<Stock> findByProductIdAndVariantIdAndStorageId(Long productId, Long variantId, Long storageId);
    Optional<Stock> findByProductIdAndVariantIdIsNullAndStorageId(Long productId, Long storageId);

    List<Stock> findByOutletId(Long outletId);
    List<Stock> findByStorageId(Long storageId);

    @Query("SELECT s FROM Stock s WHERE s.outletId = :outletId AND s.quantity <= s.minimum")
    List<Stock> findLowStockByOutletId(@Param("outletId") Long outletId);

    @Query("SELECT s FROM Stock s WHERE s.storageId = :storageId AND s.quantity <= s.minimum")
    List<Stock> findLowStockByStorageId(@Param("storageId") Long storageId);
}
