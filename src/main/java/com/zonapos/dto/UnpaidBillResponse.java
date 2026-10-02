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
public class UnpaidBillResponse {
    private Long paymentId;
    private Long trxId;
    private String trxNo;
    private LocalDateTime trxDate;
    private Double grandTotal;
    private Double remainingAmount;
    private LocalDateTime dueDate;
    private String notes;
}
