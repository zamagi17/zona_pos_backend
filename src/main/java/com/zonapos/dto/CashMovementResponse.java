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
public class CashMovementResponse {
    private Long id;
    private Long shiftId;
    private Long outletId;
    private Long userId;
    private String userName;
    private String type; // CASH_IN, CASH_OUT
    private Double amount;
    private String category;
    private String notes;
    private LocalDateTime createdAt;
}
