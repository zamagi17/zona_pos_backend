package com.zonapos.repository;

import com.zonapos.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTenantId(Long tenantId);
    List<Product> findByTenantIdAndIsActiveTrue(Long tenantId);
    Optional<Product> findByTenantIdAndSku(Long tenantId, String sku);
    Optional<Product> findByTenantIdAndBarcode(Long tenantId, String barcode);
}
