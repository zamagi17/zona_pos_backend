package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long trxId;
    private String paymentMethod;
    private Double amount;
    private String status; // PAID, UNPAID, PARTIAL, REFUNDED
    private String reference;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private String notes;
}
