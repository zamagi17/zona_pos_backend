package com.zonapos.repository;

import com.zonapos.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByTenantId(Long tenantId);
    Optional<Unit> findByTenantIdAndCode(Long tenantId, String code);
}
