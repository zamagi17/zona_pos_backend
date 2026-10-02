package com.zonapos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidationRequest {
    @NotBlank(message = "Kode voucher tidak boleh kosong")
    private String code;

    private Long outletId;

    @NotNull(message = "Subtotal belanja harus disertakan")
    private Double subtotal;
}
