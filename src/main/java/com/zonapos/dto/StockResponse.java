package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private Long variantId;
    private String variantName;
    private Long outletId;
    private String outletName;
    private Long storageId;
    private String storageName;
    private Long quantity;
    private Long minimum;
    private Boolean isLowStock;
    private LocalDateTime updatedAt;
}
