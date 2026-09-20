package com.zonapos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportDto {
    private Double totalGrossSales; // total before discount/tax
    private Double totalDiscount;
    private Double totalTax;
    private Double totalNetSales;   // total grand_total of COMPLETED transactions
    private Integer totalTransactions;
    private Double totalCashSales;
    private Double totalQrisSales;
    private Double totalTransferSales;
    private Double totalDebitSales;
    private List<DailySalesSummary> dailyBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySalesSummary {
        private String date;
        private Double totalSales;
        private Integer transactionCount;
    }
}
