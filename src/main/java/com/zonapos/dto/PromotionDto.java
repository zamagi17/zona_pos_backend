package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionDto {
    private Long id;
    private Long tenantId;
    private Long outletId;

    @NotBlank(message = "Kode voucher promo wajib diisi")
    private String code;

    @NotBlank(message = "Nama promosi wajib diisi")
    private String name;

    private String description;

    @NotBlank(message = "Tipe diskon wajib diisi (PERCENT atau FIXED)")
    private String discountType; // PERCENT, FIXED

    @NotNull(message = "Nilai diskon wajib diisi")
    @PositiveOrZero(message = "Nilai diskon tidak boleh negatif")
    private Double discountValue;

    private Double minOrderAmount;
    private Double maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer timesUsed;
    private Boolean isActive;
}
