package com.zonapos.repository;

import com.zonapos.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {
    List<TransactionItem> findByTrxId(Long trxId);
    List<TransactionItem> findByTrxIdIn(List<Long> trxIds);
    void deleteByTrxId(Long trxId);
}

