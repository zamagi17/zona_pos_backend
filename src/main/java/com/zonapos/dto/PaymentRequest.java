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
public class PaymentRequest {
    private String paymentMethod; // CASH, QRIS, TRANSFER, DEBIT, TEMPO
    private Double amount;
    private String reference;
    private LocalDateTime dueDate;
    private String notes;
}
