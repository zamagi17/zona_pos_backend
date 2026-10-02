package com.zonapos.repository;

import com.zonapos.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByTenantId(Long tenantId);

    List<Promotion> findByTenantIdAndIsActiveTrue(Long tenantId);

    Optional<Promotion> findByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCase(Long tenantId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promotion p WHERE p.tenantId = :tenantId AND UPPER(p.code) = UPPER(:code)")
    Optional<Promotion> findByTenantIdAndCodeIgnoreCaseForUpdate(@Param("tenantId") Long tenantId, @Param("code") String code);
}
