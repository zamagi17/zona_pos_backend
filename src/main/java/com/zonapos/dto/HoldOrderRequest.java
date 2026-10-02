package com.zonapos.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class HoldOrderRequest {
    @NotNull(message = "Outlet ID wajib diisi")
    private Long outletId;

    private Long shiftId;

    private Long customerId;

    private Long existingTrxId;

    @NotEmpty(message = "Item keranjang tidak boleh kosong untuk di-hold")
    private List<CartItemDto> items;

    // Klaster 3: Diskon Global Nota & Kode Voucher Promo
    private Double orderDiscount;
    private String orderDiscountType;
    private Double orderDiscountRate;
    private String voucherCode;
    private Double voucherDiscount;
}
