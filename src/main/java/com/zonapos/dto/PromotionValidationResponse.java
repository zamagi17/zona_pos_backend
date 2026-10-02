package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationResponse {
    private boolean valid;
    private String code;
    private String name;
    private String discountType; // PERCENT, FIXED
    private Double discountValue;
    private Double discountAmount;
    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private String message;
}
