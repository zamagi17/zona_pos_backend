package com.zonapos.repository;

import com.zonapos.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByTenantId(Long tenantId);
    Optional<Category> findByTenantIdAndCode(Long tenantId, String code);
}
