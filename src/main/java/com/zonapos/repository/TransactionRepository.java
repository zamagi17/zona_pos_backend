package com.zonapos.repository;

import com.zonapos.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTrxNo(String trxNo);
    List<Transaction> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<Transaction> findByOutletIdOrderByCreatedAtDesc(Long outletId);
    List<Transaction> findByOutletIdAndStatus(Long outletId, String status);
    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Transaction> findByShiftId(Long shiftId);

    @Query("SELECT t FROM Transaction t WHERE t.outletId = :outletId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByOutletIdAndDateRange(
            @Param("outletId") Long outletId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.tenantId = :tenantId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
