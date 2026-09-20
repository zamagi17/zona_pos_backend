package com.zonapos.repository;

import com.zonapos.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {
    List<StockHistory> findByStockIdOrderByCreatedAtDesc(Long stockId);
}
