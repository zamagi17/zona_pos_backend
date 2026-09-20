package com.zonapos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockPurchaseRequest {
    @NotNull(message = "Product ID wajib diisi")
    private Long productId;
    private Long variantId;
    private Long outletId;
    private Long storageId;
    @NotNull(message = "Quantity masuk wajib diisi")
    @Min(value = 1, message = "Quantity minimal 1")
    private Long quantity;
    private String remarks;
}
