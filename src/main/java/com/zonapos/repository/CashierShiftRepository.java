package com.zonapos.repository;

import com.zonapos.entity.CashierShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashierShiftRepository extends JpaRepository<CashierShift, Long> {
    Optional<CashierShift> findByUserIdAndStatus(Long userId, String status);
    List<CashierShift> findByOutletIdOrderByOpenedAtDesc(Long outletId);
}
