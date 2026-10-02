package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private Long tenantId;
    @NotBlank(message = "SKU tidak boleh kosong")
    private String sku;
    private String barcode;
    @NotBlank(message = "Nama produk tidak boleh kosong")
    private String name;
    private String desc;
    private Long categoryId;
    private String categoryName;
    private Long unitId;
    private String unitName;
    private Boolean isActive;
    // Pricing info for active outlet
    private Long priceId;
    private Double sellingPrice;
    private Double purchasePrice;
    private Short discountPercentage;
    private Double discountAmount;
    private Short taxPercentage;
    private Double taxAmount;
    // Current stock in active outlet or storage
    private Long stockQuantity;
    private Long stockMinimum;
    // Variants if any
    private List<ProductVariantDto> variants;
}
