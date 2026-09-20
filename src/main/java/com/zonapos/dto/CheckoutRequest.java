package com.zonapos.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {
    @NotNull(message = "Outlet ID wajib diisi")
    private Long outletId;

    private Long shiftId;

    private Long customerId;

    // In case this checkout is recalling an existing DRAFT transaction
    private Long existingTrxId;

    @NotEmpty(message = "Keranjang belanja tidak boleh kosong")
    private List<CartItemDto> items;

    @NotNull(message = "Metode pembayaran wajib diisi")
    private String paymentMethod; // CASH, QRIS, Transfer, Debit Card

    @NotNull(message = "Jumlah pembayaran wajib diisi")
    private Double amountPaid;

    private String reference;
}
