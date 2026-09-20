package com.zonapos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotNull(message = "Product ID wajib diisi")
    private Long productId;
    private Long variantId;
    private Long outletId;
    private Long storageId;
    @NotNull(message = "Quantity baru wajib diisi")
    private Long quantity;
    private Long minimum;
    private String remarks;
}
