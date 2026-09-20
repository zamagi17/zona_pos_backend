package com.zonapos.repository;

import com.zonapos.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {
    Optional<Price> findByProductIdAndOutletId(Long productId, Long outletId);
    List<Price> findByOutletId(Long outletId);
    List<Price> findByProductId(Long productId);
}
