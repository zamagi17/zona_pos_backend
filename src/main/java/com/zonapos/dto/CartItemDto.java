package com.zonapos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    @NotNull(message = "Product ID wajib diisi")
    private Long productId;
    private Long variantId;
    private String productName;
    private String variantName;
    @NotNull(message = "Quantity wajib diisi")
    @Min(value = 1, message = "Quantity minimal 1")
    private Integer quantity;
    private Double basePrice;
    private Double unitPrice;
    @Builder.Default
    private Double discount = 0.0;
    @Builder.Default
    private Double tax = 0.0;
    private Double subtotal;
}
