package com.zonapos.service;

import com.zonapos.dto.GrossProfitReportDto;
import com.zonapos.dto.SalesReportDto;
import com.zonapos.entity.Payment;
import com.zonapos.entity.Product;
import com.zonapos.entity.Transaction;
import com.zonapos.entity.TransactionItem;
import com.zonapos.repository.PaymentRepository;
import com.zonapos.repository.ProductRepository;
import com.zonapos.repository.TransactionItemRepository;
import com.zonapos.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    public SalesReportDto getSalesReport(Long tenantId, Long outletId, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions;
        if (outletId != null) {
            transactions = transactionRepository.findByOutletIdAndDateRange(outletId, start, end);
        } else {
            transactions = transactionRepository.findByTenantIdAndDateRange(tenantId, start, end);
        }

        double grossSales = 0.0;
        double discount = 0.0;
        double tax = 0.0;
        double netSales = 0.0;
        int completedCount = 0;

        double cashSales = 0.0;
        double qrisSales = 0.0;
        double transferSales = 0.0;
        double debitSales = 0.0;

        Map<String, double[]> dailyMap = new TreeMap<>(); // date -> [sales, count]

        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                completedCount++;
                grossSales += trx.getSubtotal();
                discount += trx.getDiscount();
                tax += trx.getTax();
                netSales += trx.getGrandTotal();

                String dateKey = trx.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                dailyMap.putIfAbsent(dateKey, new double[]{0.0, 0.0});
                dailyMap.get(dateKey)[0] += trx.getGrandTotal();
                dailyMap.get(dateKey)[1] += 1;

                List<Payment> payments = paymentRepository.findByTrxId(trx.getId());
                for (Payment p : payments) {
                    if ("PAID".equals(p.getStatus())) {
                        String method = p.getPaymentMethod().toUpperCase();
                        if (method.contains("CASH")) cashSales += p.getAmount();
                        else if (method.contains("QRIS")) qrisSales += p.getAmount();
                        else if (method.contains("TRANSFER")) transferSales += p.getAmount();
                        else if (method.contains("DEBIT") || method.contains("CARD")) debitSales += p.getAmount();
                    }
                }
            }
        }

        List<SalesReportDto.DailySalesSummary> dailyList = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : dailyMap.entrySet()) {
            dailyList.add(new SalesReportDto.DailySalesSummary(
                    entry.getKey(),
                    entry.getValue()[0],
                    (int) entry.getValue()[1]
            ));
        }

        return SalesReportDto.builder()
                .totalGrossSales(grossSales)
                .totalDiscount(discount)
                .totalTax(tax)
                .totalNetSales(netSales)
                .totalTransactions(completedCount)
                .totalCashSales(cashSales)
                .totalQrisSales(qrisSales)
                .totalTransferSales(transferSales)
                .totalDebitSales(debitSales)
                .dailyBreakdown(dailyList)
                .build();
    }

    public GrossProfitReportDto getGrossProfitReport(Long tenantId, Long outletId, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions;
        if (outletId != null) {
            transactions = transactionRepository.findByOutletIdAndDateRange(outletId, start, end);
        } else {
            transactions = transactionRepository.findByTenantIdAndDateRange(tenantId, start, end);
        }

        double totalRevenue = 0.0;
        double totalCost = 0.0;

        Map<Long, GrossProfitReportDto.ProductProfitSummary> productMap = new HashMap<>();

        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                List<TransactionItem> items = transactionItemRepository.findByTrxId(trx.getId());
                for (TransactionItem item : items) {
                    double lineRevenue = item.getSubtotal();
                    double lineCost = (item.getBasePrice() != null ? item.getBasePrice() : 0.0) * item.getQuantity();

                    totalRevenue += lineRevenue;
                    totalCost += lineCost;

                    productMap.compute(item.getProductId(), (pid, current) -> {
                        String pName = productRepository.findById(item.getProductId()).map(Product::getName).orElse("Unknown");
                        if (current == null) {
                            return new GrossProfitReportDto.ProductProfitSummary(
                                    pid,
                                    pName,
                                    item.getQuantity(),
                                    lineRevenue,
                                    lineCost,
                                    lineRevenue - lineCost,
                                    lineRevenue > 0 ? ((lineRevenue - lineCost) / lineRevenue) * 100 : 0.0
                            );
                        } else {
                            int newQty = current.getQuantitySold() + item.getQuantity();
                            double newRev = current.getTotalRevenue() + lineRevenue;
                            double newCost = current.getTotalCost() + lineCost;
                            double newProfit = newRev - newCost;
                            double newMargin = newRev > 0 ? (newProfit / newRev) * 100 : 0.0;

                            current.setQuantitySold(newQty);
                            current.setTotalRevenue(newRev);
                            current.setTotalCost(newCost);
                            current.setGrossProfit(newProfit);
                            current.setMarginPercentage(newMargin);
                            return current;
                        }
                    });
                }
            }
        }

        double totalProfit = totalRevenue - totalCost;
        double overallMargin = totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0.0;

        return GrossProfitReportDto.builder()
                .totalRevenue(totalRevenue)
                .totalCostOfGoods(totalCost)
                .totalGrossProfit(totalProfit)
                .grossProfitMarginPercentage(overallMargin)
                .productBreakdown(new ArrayList<>(productMap.values()))
                .build();
    }

    public byte[] exportSalesReportCsv(Long tenantId, Long outletId, LocalDateTime start, LocalDateTime end) {
        SalesReportDto report = getSalesReport(tenantId, outletId, start, end);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM
        sb.append("PARAMETER,NILAI\r\n");
        sb.append("Total Transaksi,").append(report.getTotalTransactions()).append("\r\n");
        sb.append("Total Omset Kotor,").append(report.getTotalGrossSales()).append("\r\n");
        sb.append("Total Diskon,").append(report.getTotalDiscount()).append("\r\n");
        sb.append("Total Pajak,").append(report.getTotalTax()).append("\r\n");
        sb.append("Total Omset Bersih,").append(report.getTotalNetSales()).append("\r\n");
        sb.append("Penjualan Tunai,").append(report.getTotalCashSales()).append("\r\n");
        sb.append("Penjualan QRIS,").append(report.getTotalQrisSales()).append("\r\n");
        sb.append("Penjualan Transfer Bank,").append(report.getTotalTransferSales()).append("\r\n");
        sb.append("Penjualan Debit EDC,").append(report.getTotalDebitSales()).append("\r\n");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
