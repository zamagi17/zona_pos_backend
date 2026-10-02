package com.zonapos.repository;

import com.zonapos.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByTenantIdOrderByIsActiveDescNameAsc(Long tenantId);
    List<Supplier> findByTenantIdAndIsActiveTrueOrderByNameAsc(Long tenantId);
    Optional<Supplier> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndNameIgnoreCase(Long tenantId, String name);
}
