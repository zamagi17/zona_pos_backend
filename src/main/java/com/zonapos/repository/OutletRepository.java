package com.zonapos.repository;

import com.zonapos.entity.Outlet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutletRepository extends JpaRepository<Outlet, Long> {
    List<Outlet> findByTenantId(Long tenantId);
}
