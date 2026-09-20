package com.zonapos.repository;

import com.zonapos.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageRepository extends JpaRepository<Storage, Long> {
    List<Storage> findByTenantId(Long tenantId);
    Optional<Storage> findByTenantIdAndCode(Long tenantId, String code);
}
