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

    // Single payment mode (backward compatible)
    private String paymentMethod; // CASH, QRIS, Transfer, Debit Card, TEMPO

    private Double amountPaid;

    private String reference;

    private String idempotencyKey;

    // Split payment & Kasbon/Tempo support
    private java.util.List<PaymentRequest> payments;

    private java.time.LocalDateTime dueDate;

    // Klaster 3: Diskon Global Nota & Kode Voucher Promo
    private Double orderDiscount; // Nominal rupiah diskon nota
    private String orderDiscountType; // PERCENT, FIXED
    private Double orderDiscountRate; // Persentase (e.g. 10) atau nominal
    private String voucherCode;
    private Double voucherDiscount; // Nominal potongan voucher
}

