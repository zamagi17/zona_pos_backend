package com.zonapos.repository;

import com.zonapos.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findByShiftIdOrderByCreatedAtDesc(Long shiftId);
    List<CashMovement> findByShiftIdInOrderByCreatedAtDesc(List<Long> shiftIds);
    List<CashMovement> findByOutletIdOrderByCreatedAtDesc(Long outletId);

    @Query("SELECT COALESCE(SUM(cm.amount), 0.0) FROM CashMovement cm WHERE cm.shiftId = :shiftId AND cm.type = :type")
    Double sumAmountByShiftIdAndType(@Param("shiftId") Long shiftId, @Param("type") String type);
}
