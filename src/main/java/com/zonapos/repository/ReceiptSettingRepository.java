package com.zonapos.repository;

import com.zonapos.entity.ReceiptSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptSettingRepository extends JpaRepository<ReceiptSetting, Long> {
    Optional<ReceiptSetting> findByTenantIdAndOutletId(Long tenantId, Long outletId);

    Optional<ReceiptSetting> findByTenantIdAndOutletIdIsNull(Long tenantId);

    List<ReceiptSetting> findByTenantId(Long tenantId);
}
