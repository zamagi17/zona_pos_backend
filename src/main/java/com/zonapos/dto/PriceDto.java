package com.zonapos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDto {
    private Long id;
    @NotNull(message = "Product ID wajib diisi")
    private Long productId;
    @NotNull(message = "Outlet ID wajib diisi")
    private Long outletId;
    @NotNull(message = "Selling price wajib diisi")
    private Double sellingPrice;
    @NotNull(message = "Purchase price wajib diisi")
    private Double purchasePrice;
    private Short discountPercentage;
    private Double discountAmount;
    private Short taxPercentage;
    private Double taxAmount;
}
