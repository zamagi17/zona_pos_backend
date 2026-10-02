package com.zonapos.repository;

import com.zonapos.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTrxId(Long trxId);
    List<Payment> findByTrxIdIn(List<Long> trxIds);
    Optional<Payment> findByReference(String reference);

    @org.springframework.data.jpa.repository.Query("SELECT t.customerId AS customerId, SUM(p.amount) AS totalDebt, COUNT(p.id) AS unpaidCount " +
           "FROM Payment p, Transaction t " +
           "WHERE p.trxId = t.id AND t.tenantId = :tenantId AND p.status IN ('UNPAID', 'PARTIAL') AND t.customerId IS NOT NULL " +
           "GROUP BY t.customerId")
    List<CustomerDebtSummary> findReceivablesSummaryByTenantId(@org.springframework.data.repository.query.Param("tenantId") Long tenantId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p, Transaction t " +
           "WHERE p.trxId = t.id AND t.customerId = :customerId AND p.status IN ('UNPAID', 'PARTIAL') " +
           "ORDER BY t.createdAt ASC")
    List<Payment> findUnpaidByCustomerId(@org.springframework.data.repository.query.Param("customerId") Long customerId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p, Transaction t " +
           "WHERE p.trxId = t.id AND t.customerId = :customerId AND t.id = :trxId AND p.status IN ('UNPAID', 'PARTIAL') " +
           "ORDER BY p.id ASC")
    List<Payment> findUnpaidByCustomerIdAndTrxId(@org.springframework.data.repository.query.Param("customerId") Long customerId,
                                                @org.springframework.data.repository.query.Param("trxId") Long trxId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0.0) FROM Payment p, Transaction t " +
           "WHERE p.trxId = t.id AND t.customerId = :customerId AND p.status IN ('UNPAID', 'PARTIAL')")
    Double sumUnpaidAmountByCustomerId(@org.springframework.data.repository.query.Param("customerId") Long customerId);
}


