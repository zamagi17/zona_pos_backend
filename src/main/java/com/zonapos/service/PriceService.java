package com.zonapos.service;

import com.zonapos.dto.PriceDto;
import com.zonapos.entity.Price;
import com.zonapos.entity.PriceHistory;
import com.zonapos.entity.User;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.PriceHistoryRepository;
import com.zonapos.repository.PriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceService {

    private final PriceRepository priceRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    @Transactional
    public PriceDto setOutletPrice(PriceDto dto, User currentUser) {
        Price price = priceRepository.findByProductIdAndOutletId(dto.getProductId(), dto.getOutletId())
                .orElseGet(() -> Price.builder()
                        .productId(dto.getProductId())
                        .outletId(dto.getOutletId())
                        .createdBy(currentUser.getName())
                        .build());

        price.setSellingPrice(dto.getSellingPrice());
        price.setPurchasePrice(dto.getPurchasePrice());
        price.setDiscountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0);
        price.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : 0.0);
        price.setTaxPercentage(dto.getTaxPercentage() != null ? dto.getTaxPercentage() : 0);
        price.setTaxAmount(dto.getTaxAmount() != null ? dto.getTaxAmount() : 0.0);
        price.setUpdatedBy(currentUser.getName());

        price = priceRepository.save(price);

        // Record to Price History
        PriceHistory history = PriceHistory.builder()
                .priceId(price.getId())
                .sellingPrice(price.getSellingPrice())
                .purchasePrice(price.getPurchasePrice())
                .discountPercentage(price.getDiscountPercentage())
                .discountAmount(price.getDiscountAmount())
                .taxPercentage(price.getTaxPercentage())
                .taxAmount(price.getTaxAmount())
                .userId(currentUser.getId())
                .build();
        priceHistoryRepository.save(history);

        dto.setId(price.getId());
        return dto;
    }

    public List<PriceHistory> getPriceHistory(Long priceId) {
        return priceHistoryRepository.findByPriceIdOrderByCreatedAtDesc(priceId);
    }
}
