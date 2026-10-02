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
public class CreateCashMovementRequest {

    private Long shiftId;

    @NotBlank(message = "Tipe mutasi kas wajib diisi (CASH_IN atau CASH_OUT)")
    private String type; // CASH_IN, CASH_OUT

    @NotNull(message = "Nominal uang wajib diisi")
    @DecimalMin(value = "1.0", message = "Nominal uang harus lebih besar dari 0")
    private Double amount;

    @NotBlank(message = "Kategori mutasi kas wajib diisi")
    private String category; // Operasional, Bahan, Tambah Modal, Kurir, Lainnya

    private String notes; // Catatan Keterangan
}
