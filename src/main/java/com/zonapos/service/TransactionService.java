package com.zonapos.service;

import com.zonapos.dto.*;
import com.zonapos.entity.*;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final StockRepository stockRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final CashierShiftRepository cashierShiftRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OutletRepository outletRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse holdOrder(HoldOrderRequest request, User cashier) {
        Transaction transaction;
        if (request.getExistingTrxId() != null) {
            transaction = transactionRepository.findById(request.getExistingTrxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Draft transaksi tidak ditemukan"));
            // Clear existing items to rewrite
            transactionItemRepository.deleteByTrxId(transaction.getId());
        } else {
            transaction = Transaction.builder()
                    .trxNo(generateTrxNo())
                    .tenantId(cashier.getTenantId())
                    .outletId(request.getOutletId())
                    .userId(cashier.getId())
                    .customerId(request.getCustomerId())
                    .shiftId(request.getShiftId())
                    .createdBy(cashier.getName())
                    .build();
        }

        double subtotal = 0.0;
        double discount = 0.0;
        double tax = 0.0;

        for (CartItemDto item : request.getItems()) {
            double lineSubtotal = (item.getUnitPrice() * item.getQuantity()) - (item.getDiscount() != null ? item.getDiscount() : 0.0);
            subtotal += lineSubtotal;
            discount += (item.getDiscount() != null ? item.getDiscount() : 0.0);
            tax += (item.getTax() != null ? item.getTax() : 0.0);
        }

        transaction.setSubtotal(subtotal);
        transaction.setDiscount(discount);
        transaction.setTax(tax);
        transaction.setGrandTotal(subtotal + tax);
        transaction.setStatus("DRAFT");
        transaction = transactionRepository.save(transaction);

        // Save items
        saveTransactionItems(transaction.getId(), request.getOutletId(), request.getItems(), cashier.getName());

        return mapToTransactionResponse(transaction, null);
    }

    @Transactional
    public TransactionResponse checkout(CheckoutRequest request, User cashier) {
        // 1. Validate active shift
        Long shiftId = request.getShiftId();
        if (shiftId == null) {
            Optional<CashierShift> activeShift = cashierShiftRepository.findByUserIdAndStatus(cashier.getId(), "OPEN");
            if (activeShift.isEmpty()) {
                throw new BadRequestException("Shift kasir belum dibuka. Buka shift terlebih dahulu sebelum melayani transaksi.");
            }
            shiftId = activeShift.get().getId();
        }

        Transaction transaction;
        if (request.getExistingTrxId() != null) {
            transaction = transactionRepository.findById(request.getExistingTrxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Draft pesanan tidak ditemukan"));
            transactionItemRepository.deleteByTrxId(transaction.getId());
        } else {
            transaction = Transaction.builder()
                    .trxNo(generateTrxNo())
                    .tenantId(cashier.getTenantId())
                    .outletId(request.getOutletId())
                    .userId(cashier.getId())
                    .createdBy(cashier.getName())
                    .build();
        }

        transaction.setCustomerId(request.getCustomerId());
        transaction.setShiftId(shiftId);

        // Calculate Totals
        double subtotal = 0.0;
        double discount = 0.0;
        double tax = 0.0;

        for (CartItemDto item : request.getItems()) {
            double lineSubtotal = (item.getUnitPrice() * item.getQuantity()) - (item.getDiscount() != null ? item.getDiscount() : 0.0);
            subtotal += lineSubtotal;
            discount += (item.getDiscount() != null ? item.getDiscount() : 0.0);
            tax += (item.getTax() != null ? item.getTax() : 0.0);
        }

        double grandTotal = subtotal + tax;

        if (request.getAmountPaid() < grandTotal) {
            throw new BadRequestException("Nominal pembayaran kurang dari total tagihan (Rp " + grandTotal + ")");
        }

        transaction.setSubtotal(subtotal);
        transaction.setDiscount(discount);
        transaction.setTax(tax);
        transaction.setGrandTotal(grandTotal);
        transaction.setStatus("COMPLETED");
        transaction = transactionRepository.save(transaction);

        // 2. Save items and atomically deduct inventory
        for (CartItemDto item : request.getItems()) {
            TransactionItem tItem = TransactionItem.builder()
                    .trxId(transaction.getId())
                    .outletId(request.getOutletId())
                    .productId(item.getProductId())
                    .variantId(item.getVariantId())
                    .quantity(item.getQuantity())
                    .basePrice(item.getBasePrice() != null ? item.getBasePrice() : 0.0)
                    .unitPrice(item.getUnitPrice())
                    .discount(item.getDiscount() != null ? item.getDiscount() : 0.0)
                    .tax(item.getTax() != null ? item.getTax() : 0.0)
                    .subtotal((item.getUnitPrice() * item.getQuantity()) - (item.getDiscount() != null ? item.getDiscount() : 0.0))
                    .createdBy(cashier.getName())
                    .build();
            transactionItemRepository.save(tItem);

            // Deduct Stock
            deductStock(request.getOutletId(), item.getProductId(), item.getVariantId(), item.getQuantity(),
                    transaction.getTrxNo(), cashier);
        }

        // 3. Save Payment
        Payment payment = Payment.builder()
                .trxId(transaction.getId())
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .amount(request.getAmountPaid())
                .status("PAID")
                .reference(request.getReference())
                .paidAt(LocalDateTime.now())
                .createdBy(cashier.getName())
                .build();
        payment = paymentRepository.save(payment);

        // 4. Save Transaction History
        TransactionHistory history = TransactionHistory.builder()
                .trxId(transaction.getId())
                .trxNo(transaction.getTrxNo())
                .outletId(transaction.getOutletId())
                .customerId(transaction.getCustomerId())
                .subtotal(transaction.getSubtotal())
                .discount(transaction.getDiscount())
                .tax(transaction.getTax())
                .grandTotal(transaction.getGrandTotal())
                .status(transaction.getStatus())
                .userId(cashier.getId())
                .build();
        transactionHistoryRepository.save(history);

        double change = request.getAmountPaid() - grandTotal;
        TransactionResponse response = mapToTransactionResponse(transaction, payment);
        response.setChangeAmount(change);
        return response;
    }

    @Transactional
    public TransactionResponse refund(Long trxId, RefundRequest request, User currentUser) {
        Transaction transaction = transactionRepository.findById(trxId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan dengan ID: " + trxId));

        if (!"COMPLETED".equals(transaction.getStatus())) {
            throw new BadRequestException("Hanya transaksi COMPLETED yang dapat di-refund");
        }

        transaction.setStatus("REFUNDED");
        transaction.setUpdatedBy(currentUser.getName());
        transaction = transactionRepository.save(transaction);

        // 1. Restore Inventory
        List<TransactionItem> items = transactionItemRepository.findByTrxId(transaction.getId());
        for (TransactionItem item : items) {
            restoreStock(transaction.getOutletId(), item.getProductId(), item.getVariantId(), item.getQuantity(),
                    transaction.getTrxNo(), request.getReason(), currentUser);
        }

        // 2. Update Payments to REFUNDED
        List<Payment> payments = paymentRepository.findByTrxId(transaction.getId());
        for (Payment p : payments) {
            p.setStatus("REFUNDED");
            p.setUpdatedBy(currentUser.getName());
            paymentRepository.save(p);
        }

        // 3. Record to Transaction History
        TransactionHistory history = TransactionHistory.builder()
                .trxId(transaction.getId())
                .trxNo(transaction.getTrxNo())
                .outletId(transaction.getOutletId())
                .customerId(transaction.getCustomerId())
                .subtotal(transaction.getSubtotal())
                .discount(transaction.getDiscount())
                .tax(transaction.getTax())
                .grandTotal(transaction.getGrandTotal())
                .status("REFUNDED")
                .userId(currentUser.getId())
                .build();
        transactionHistoryRepository.save(history);

        return mapToTransactionResponse(transaction, payments.isEmpty() ? null : payments.get(0));
    }

    public List<TransactionResponse> getTransactions(Long outletId, String status) {
        List<Transaction> list;
        if (status != null && !status.isEmpty()) {
            list = transactionRepository.findByOutletIdAndStatus(outletId, status);
        } else {
            list = transactionRepository.findByOutletIdOrderByCreatedAtDesc(outletId);
        }
        return list.stream().map(t -> mapToTransactionResponse(t, null)).collect(Collectors.toList());
    }

    public TransactionResponse getTransactionDetail(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan"));
        List<Payment> payments = paymentRepository.findByTrxId(t.getId());
        return mapToTransactionResponse(t, payments.isEmpty() ? null : payments.get(0));
    }

    private void deductStock(Long outletId, Long productId, Long variantId, int qty, String trxNo, User cashier) {
        Optional<Stock> stockOpt;
        if (variantId != null) {
            stockOpt = stockRepository.findByProductIdAndVariantIdAndOutletId(productId, variantId, outletId);
        } else {
            stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(productId, outletId);
        }

        Stock stock = stockOpt.orElseThrow(() -> new BadRequestException("Stok produk tidak terdaftar di outlet ini"));

        if (stock.getQuantity() < qty) {
            String pName = productRepository.findById(productId).map(Product::getName).orElse("Produk");
            throw new BadRequestException("Stok tidak mencukupi untuk: " + pName + " (Tersedia: " + stock.getQuantity() + ")");
        }

        long newQty = stock.getQuantity() - qty;
        stock.setQuantity(newQty);
        stock.setUpdatedBy(cashier.getName());
        stockRepository.save(stock);

        StockHistory history = StockHistory.builder()
                .stockId(stock.getId())
                .stockInOut((long) -qty)
                .quantity(newQty)
                .status("OUT")
                .type("SALE")
                .remarks("Penjualan POS No. " + trxNo)
                .userId(cashier.getId())
                .build();
        stockHistoryRepository.save(history);
    }

    private void restoreStock(Long outletId, Long productId, Long variantId, int qty, String trxNo, String reason, User user) {
        Optional<Stock> stockOpt;
        if (variantId != null) {
            stockOpt = stockRepository.findByProductIdAndVariantIdAndOutletId(productId, variantId, outletId);
        } else {
            stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndOutletId(productId, outletId);
        }

        if (stockOpt.isPresent()) {
            Stock stock = stockOpt.get();
            long newQty = stock.getQuantity() + qty;
            stock.setQuantity(newQty);
            stock.setUpdatedBy(user.getName());
            stockRepository.save(stock);

            StockHistory history = StockHistory.builder()
                    .stockId(stock.getId())
                    .stockInOut((long) qty)
                    .quantity(newQty)
                    .status("IN")
                    .type("REFUND")
                    .remarks("Pengembalian stok refund POS " + trxNo + ": " + reason)
                    .userId(user.getId())
                    .build();
            stockHistoryRepository.save(history);
        }
    }

    private void saveTransactionItems(Long trxId, Long outletId, List<CartItemDto> items, String author) {
        for (CartItemDto item : items) {
            TransactionItem tItem = TransactionItem.builder()
                    .trxId(trxId)
                    .outletId(outletId)
                    .productId(item.getProductId())
                    .variantId(item.getVariantId())
                    .quantity(item.getQuantity())
                    .basePrice(item.getBasePrice() != null ? item.getBasePrice() : 0.0)
                    .unitPrice(item.getUnitPrice())
                    .discount(item.getDiscount() != null ? item.getDiscount() : 0.0)
                    .tax(item.getTax() != null ? item.getTax() : 0.0)
                    .subtotal((item.getUnitPrice() * item.getQuantity()) - (item.getDiscount() != null ? item.getDiscount() : 0.0))
                    .createdBy(author)
                    .build();
            transactionItemRepository.save(tItem);
        }
    }

    private TransactionResponse mapToTransactionResponse(Transaction t, Payment payment) {
        String outletName = outletRepository.findById(t.getOutletId()).map(Outlet::getName).orElse(null);
        String cashierName = userRepository.findById(t.getUserId()).map(User::getName).orElse(null);
        String customerName = t.getCustomerId() != null ?
                customerRepository.findById(t.getCustomerId()).map(Customer::getName).orElse(null) : "Umum";

        List<CartItemDto> items = transactionItemRepository.findByTrxId(t.getId()).stream()
                .map(ti -> {
                    String pName = productRepository.findById(ti.getProductId()).map(Product::getName).orElse(null);
                    String vName = ti.getVariantId() != null ?
                            productVariantRepository.findById(ti.getVariantId()).map(ProductVariant::getName).orElse(null) : null;
                    return CartItemDto.builder()
                            .productId(ti.getProductId())
                            .variantId(ti.getVariantId())
                            .productName(pName)
                            .variantName(vName)
                            .quantity(ti.getQuantity())
                            .basePrice(ti.getBasePrice())
                            .unitPrice(ti.getUnitPrice())
                            .discount(ti.getDiscount())
                            .tax(ti.getTax())
                            .subtotal(ti.getSubtotal())
                            .build();
                }).collect(Collectors.toList());

        if (payment == null) {
            List<Payment> pList = paymentRepository.findByTrxId(t.getId());
            if (!pList.isEmpty()) {
                payment = pList.get(0);
            }
        }

        return TransactionResponse.builder()
                .id(t.getId())
                .trxNo(t.getTrxNo())
                .tenantId(t.getTenantId())
                .outletId(t.getOutletId())
                .outletName(outletName)
                .userId(t.getUserId())
                .cashierName(cashierName)
                .customerId(t.getCustomerId())
                .customerName(customerName)
                .shiftId(t.getShiftId())
                .subtotal(t.getSubtotal())
                .discount(t.getDiscount())
                .tax(t.getTax())
                .grandTotal(t.getGrandTotal())
                .status(t.getStatus())
                .paymentMethod(payment != null ? payment.getPaymentMethod() : null)
                .paymentAmount(payment != null ? payment.getAmount() : null)
                .changeAmount(payment != null && payment.getAmount() >= t.getGrandTotal() ? payment.getAmount() - t.getGrandTotal() : 0.0)
                .paymentStatus(payment != null ? payment.getStatus() : null)
                .createdAt(t.getCreatedAt())
                .items(items)
                .build();
    }

    private String generateTrxNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        return "TRX-" + timestamp + "-" + rand;
    }
}
