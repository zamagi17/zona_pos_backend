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
public class ShiftResponse {
    private Long id;
    private Long outletId;
    private String outletName;
    private Long userId;
    private String cashierName;
    private Double startCash;
    private Double totalCashSales;
    private Double totalNonCashSales;
    private Integer totalTransactions;
    private Double totalCashIn;
    private Double totalCashOut;
    private Double expectedCash;
    private Double actualCash;
    private Double cashDifference; // actual - expected
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private java.util.List<CashMovementResponse> cashMovements;
}
