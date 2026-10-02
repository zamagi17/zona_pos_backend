package com.zonapos.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class SettleDebtRequest {
    private Long trxId;

    @NotNull(message = "Nominal pelunasan wajib diisi")
    @DecimalMin(value = "1.0", message = "Nominal pelunasan harus lebih besar dari Rp 0")
    private Double amount;

    @NotBlank(message = "Metode pembayaran wajib diisi (CASH, QRIS, TRANSFER, DEBIT)")
    private String paymentMethod; // CASH, QRIS, TRANSFER, DEBIT

    private String notes;
}
