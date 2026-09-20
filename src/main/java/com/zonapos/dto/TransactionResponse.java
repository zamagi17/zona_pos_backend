package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String trxNo;
    private Long tenantId;
    private Long outletId;
    private String outletName;
    private Long userId;
    private String cashierName;
    private Long customerId;
    private String customerName;
    private Long shiftId;
    private Double subtotal;
    private Double discount;
    private Double tax;
    private Double grandTotal;
    private String status;
    private String paymentMethod;
    private Double paymentAmount;
    private Double changeAmount;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private List<CartItemDto> items;
}
