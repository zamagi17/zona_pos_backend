package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantDto {
    private Long id;
    private Long productId;
    @NotBlank(message = "SKU varian tidak boleh kosong")
    private String sku;
    @NotBlank(message = "Nama varian tidak boleh kosong")
    private String name;
    private Long stockQuantity;
}
