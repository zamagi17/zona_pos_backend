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
public class GrossProfitReportDto {
    private Double totalRevenue;     // grand total revenue
    private Double totalCostOfGoods; // total purchase price / COGS
    private Double totalGrossProfit; // totalRevenue - totalCostOfGoods
    private Double grossProfitMarginPercentage;
    private List<ProductProfitSummary> productBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductProfitSummary {
        private Long productId;
        private String productName;
        private Integer quantitySold;
        private Double totalRevenue;
        private Double totalCost;
        private Double grossProfit;
        private Double marginPercentage;
    }
}
