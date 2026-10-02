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
import java.util.*;
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
    private final PromotionRepository promotionRepository;

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

        double itemsGrossSubtotal = 0.0;
        double itemDiscountTotal = 0.0;
        double tax = 0.0;

        for (CartItemDto item : request.getItems()) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
            double itemDisc = (item.getDiscount() != null ? item.getDiscount() : 0.0) * qty;
            itemsGrossSubtotal += (unitPrice * qty);
            itemDiscountTotal += itemDisc;
            tax += (item.getTax() != null ? item.getTax() * qty : 0.0);
        }

        double netItemsSubtotal = Math.max(0.0, itemsGrossSubtotal - itemDiscountTotal);

        double orderDiscount = 0.0;
        if (request.getOrderDiscountType() != null && "PERCENT".equalsIgnoreCase(request.getOrderDiscountType())) {
            double rate = request.getOrderDiscountRate() != null ? request.getOrderDiscountRate() : 0.0;
            orderDiscount = Math.round((netItemsSubtotal * rate) / 100.0);
        } else if (request.getOrderDiscount() != null) {
            orderDiscount = Math.min(netItemsSubtotal, request.getOrderDiscount());
        }

        double voucherDiscount = 0.0;
        if (request.getVoucherDiscount() != null) {
            voucherDiscount = Math.min(Math.max(0.0, netItemsSubtotal - orderDiscount), request.getVoucherDiscount());
        }

        double totalDiscount = itemDiscountTotal + orderDiscount + voucherDiscount;
        double grandTotal = Math.max(0.0, itemsGrossSubtotal - totalDiscount + tax);

        transaction.setSubtotal(itemsGrossSubtotal);
        transaction.setDiscount(totalDiscount);
        transaction.setOrderDiscount(orderDiscount);
        transaction.setOrderDiscountType(request.getOrderDiscountType());
        transaction.setOrderDiscountRate(request.getOrderDiscountRate());
        transaction.setVoucherCode(request.getVoucherCode() != null ? request.getVoucherCode().trim().toUpperCase() : null);
        transaction.setVoucherDiscount(voucherDiscount);
        transaction.setTax(tax);
        transaction.setGrandTotal(grandTotal);
        transaction.setStatus("DRAFT");
        transaction = transactionRepository.save(transaction);

        // Save items
        saveTransactionItems(transaction.getId(), request.getOutletId(), request.getItems(), cashier.getName());

        return mapToTransactionResponse(transaction, null);
    }

    @Transactional
    public TransactionResponse checkout(CheckoutRequest request, User cashier) {
        // 0. Idempotency Check: Prevent duplicate payment processing if cashier clicks repeatedly or network retries
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Payment> existingPayment = paymentRepository.findByReference(request.getIdempotencyKey().trim());
            if (existingPayment.isPresent()) {
                Payment p = existingPayment.get();
                Transaction existingTrx = transactionRepository.findById(p.getTrxId()).orElse(null);
                if (existingTrx != null && "COMPLETED".equals(existingTrx.getStatus())) {
                    return mapToTransactionResponse(existingTrx, p);
                }
            }
        }

        // 1. Validate active shift
        Long shiftId = request.getShiftId();
        if (shiftId == null) {
            Optional<CashierShift> activeShift = cashierShiftRepository.findByUserIdAndStatus(cashier.getId(), "OPEN");
            if (activeShift.isEmpty()) {
                throw new BadRequestException("Shift kasir belum dibuka. Buka shift terlebih dahulu sebelum melayani transaksi.");
            }
            shiftId = activeShift.get().getId();
        }

        // 2. Validate Payments (Split or Single)
        List<PaymentRequest> paymentRequests = request.getPayments();
        if (paymentRequests == null || paymentRequests.isEmpty()) {
            if (request.getPaymentMethod() == null || request.getAmountPaid() == null) {
                throw new BadRequestException("Metode pembayaran dan nominal pembayaran wajib diisi.");
            }
            paymentRequests = new ArrayList<>();
            paymentRequests.add(PaymentRequest.builder()
                    .paymentMethod(request.getPaymentMethod())
                    .amount(request.getAmountPaid())
                    .reference((request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                            ? request.getIdempotencyKey().trim()
                            : request.getReference())
                    .dueDate(request.getDueDate())
                    .build());
        }

        // Validate customer for TEMPO / KASBON
        boolean hasTempo = paymentRequests.stream().anyMatch(p ->
                "TEMPO".equalsIgnoreCase(p.getPaymentMethod()) || "KASBON".equalsIgnoreCase(p.getPaymentMethod()));
        if (hasTempo) {
            if (request.getCustomerId() == null) {
                throw new BadRequestException("Pembayaran Kasbon / Tempo hanya dapat digunakan untuk pelanggan yang terdaftar. Silakan pilih atau daftarkan pelanggan terlebih dahulu.");
            }
            Customer cust = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pelanggan tidak ditemukan"));
            if (!cust.getTenantId().equals(cashier.getTenantId())) {
                throw new BadRequestException("Pelanggan tidak valid untuk tenant ini.");
            }
        }

        // 3. Create or Update Transaction
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
        if (request.getDueDate() != null) {
            transaction.setDueDate(request.getDueDate());
        }

        // Calculate Totals
        double itemsGrossSubtotal = 0.0;
        double itemDiscountTotal = 0.0;
        double tax = 0.0;

        for (CartItemDto item : request.getItems()) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            double unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : 0.0;
            double itemDisc = (item.getDiscount() != null ? item.getDiscount() : 0.0) * qty;
            itemsGrossSubtotal += (unitPrice * qty);
            itemDiscountTotal += itemDisc;
            tax += (item.getTax() != null ? item.getTax() * qty : 0.0);
        }

        double netItemsSubtotal = Math.max(0.0, itemsGrossSubtotal - itemDiscountTotal);

        // 1. Order Discount (Diskon Nota Global)
        double orderDiscount = 0.0;
        if (request.getOrderDiscountType() != null && "PERCENT".equalsIgnoreCase(request.getOrderDiscountType())) {
            double rate = request.getOrderDiscountRate() != null ? request.getOrderDiscountRate() : 0.0;
            orderDiscount = Math.round((netItemsSubtotal * rate) / 100.0);
        } else if (request.getOrderDiscount() != null) {
            orderDiscount = Math.min(netItemsSubtotal, request.getOrderDiscount());
        }

        // 2. Voucher Promo (Voucher Kupon) with Concurrency Safety (Pessimistic Lock)
        double voucherDiscount = 0.0;
        String voucherCode = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucherCode = request.getVoucherCode().trim().toUpperCase();
            Optional<Promotion> promoOpt = promotionRepository.findByTenantIdAndCodeIgnoreCaseForUpdate(cashier.getTenantId(), voucherCode);
            if (promoOpt.isPresent()) {
                Promotion promo = promoOpt.get();
                LocalDateTime now = LocalDateTime.now();
                boolean dateValid = (promo.getStartDate() == null || !now.isBefore(promo.getStartDate()))
                        && (promo.getEndDate() == null || !now.isAfter(promo.getEndDate()));
                boolean quotaValid = (promo.getUsageLimit() == null || promo.getTimesUsed() < promo.getUsageLimit());
                boolean outletValid = (promo.getOutletId() == null || promo.getOutletId().equals(cashier.getOutletId()));

                if (Boolean.TRUE.equals(promo.getIsActive()) && dateValid && quotaValid && outletValid) {
                    double eligibleSubtotal = Math.max(0.0, netItemsSubtotal - orderDiscount);
                    double minReq = promo.getMinOrderAmount() != null ? promo.getMinOrderAmount() : 0.0;
                    if (eligibleSubtotal >= minReq) {
                        if ("PERCENT".equalsIgnoreCase(promo.getDiscountType())) {
                            voucherDiscount = (eligibleSubtotal * (promo.getDiscountValue() != null ? promo.getDiscountValue() : 0.0)) / 100.0;
                            if (promo.getMaxDiscountAmount() != null && promo.getMaxDiscountAmount() > 0 && voucherDiscount > promo.getMaxDiscountAmount()) {
                                voucherDiscount = promo.getMaxDiscountAmount();
                            }
                            voucherDiscount = Math.round(voucherDiscount);
                        } else {
                            voucherDiscount = Math.min(eligibleSubtotal, promo.getDiscountValue() != null ? promo.getDiscountValue() : 0.0);
                        }

                        // Increment voucher usage atomically
                        promo.setTimesUsed(promo.getTimesUsed() + 1);
                        promotionRepository.save(promo);
                    }
                }
            } else if (request.getVoucherDiscount() != null && request.getVoucherDiscount() > 0) {
                voucherDiscount = Math.min(Math.max(0.0, netItemsSubtotal - orderDiscount), request.getVoucherDiscount());
            }
        }

        double totalDiscount = itemDiscountTotal + orderDiscount + voucherDiscount;
        double grandTotal = Math.max(0.0, itemsGrossSubtotal - totalDiscount + tax);

        double totalTendered = paymentRequests.stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        if (totalTendered < grandTotal) {
            throw new BadRequestException(String.format("Nominal pembayaran (Rp %,.0f) kurang dari total tagihan (Rp %,.0f)", totalTendered, grandTotal));
        }

        double totalChange = totalTendered - grandTotal;

        transaction.setSubtotal(itemsGrossSubtotal);
        transaction.setDiscount(totalDiscount);
        transaction.setOrderDiscount(orderDiscount);
        transaction.setOrderDiscountType(request.getOrderDiscountType());
        transaction.setOrderDiscountRate(request.getOrderDiscountRate());
        transaction.setVoucherCode(voucherCode);
        transaction.setVoucherDiscount(voucherDiscount);
        transaction.setTax(tax);
        transaction.setGrandTotal(grandTotal);
        transaction.setStatus("COMPLETED");
        transaction = transactionRepository.save(transaction);

        // 4. Save items and atomically deduct inventory with Pessimistic Lock
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

            // Deduct Stock with Concurrency Lock
            deductStock(request.getOutletId(), item.getProductId(), item.getVariantId(), item.getQuantity(),
                    transaction.getTrxNo(), cashier);
        }

        // 5. Save Payments
        List<Payment> savedPayments = new ArrayList<>();
        double remainingChangeToDeduct = totalChange;

        for (PaymentRequest pr : paymentRequests) {
            String method = pr.getPaymentMethod().toUpperCase();
            boolean isTempo = "TEMPO".equals(method) || "KASBON".equals(method);
            String status = isTempo ? "UNPAID" : "PAID";
            LocalDateTime paidAt = isTempo ? null : LocalDateTime.now();
            LocalDateTime pDueDate = pr.getDueDate() != null ? pr.getDueDate() : request.getDueDate();
            if (isTempo && pDueDate != null && transaction.getDueDate() == null) {
                transaction.setDueDate(pDueDate);
                transaction = transactionRepository.save(transaction);
            }

            double originalTender = pr.getAmount() != null ? pr.getAmount() : 0.0;
            double netAmount = originalTender;
            String note = pr.getNotes();

            // If cash and there is change, adjust net cash saved in payment so cash sales match drawer exactly
            if ("CASH".equals(method) && remainingChangeToDeduct > 0) {
                double deduct = Math.min(netAmount, remainingChangeToDeduct);
                netAmount -= deduct;
                remainingChangeToDeduct -= deduct;
                String changeInfo = String.format("Diterima: Rp %,.0f (Kembalian: Rp %,.0f)", originalTender, deduct);
                note = (note != null && !note.isBlank()) ? note + " | " + changeInfo : changeInfo;
            }

            String ref = pr.getReference();
            if (ref == null || ref.isBlank()) {
                ref = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                        ? request.getIdempotencyKey().trim()
                        : request.getReference();
            }

            Payment payment = Payment.builder()
                    .trxId(transaction.getId())
                    .paymentMethod(method)
                    .amount(netAmount)
                    .status(status)
                    .reference(ref)
                    .dueDate(pDueDate)
                    .notes(note)
                    .paidAt(paidAt)
                    .createdBy(cashier.getName())
                    .build();
            savedPayments.add(paymentRepository.save(payment));
        }

        // 6. Save Transaction History
        TransactionHistory history = TransactionHistory.builder()
                .trxId(transaction.getId())
                .trxNo(transaction.getTrxNo())
                .outletId(transaction.getOutletId())
                .customerId(transaction.getCustomerId())
                .subtotal(transaction.getSubtotal())
                .discount(transaction.getDiscount())
                .orderDiscount(transaction.getOrderDiscount())
                .voucherCode(transaction.getVoucherCode())
                .voucherDiscount(transaction.getVoucherDiscount())
                .tax(transaction.getTax())
                .grandTotal(transaction.getGrandTotal())
                .status(transaction.getStatus())
                .userId(cashier.getId())
                .build();
        transactionHistoryRepository.save(history);

        TransactionResponse response = mapToTransactionResponse(transaction, savedPayments.isEmpty() ? null : savedPayments.get(0));
        response.setPaymentAmount(totalTendered);
        response.setChangeAmount(totalChange);
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

        // 1. Restore Inventory with Lock
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

        if (list.isEmpty()) {
            return Collections.emptyList();
        }

        return mapToTransactionResponseBatch(list);
    }

    public TransactionResponse getTransactionDetail(Long id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaksi tidak ditemukan"));
        List<Payment> payments = paymentRepository.findByTrxId(t.getId());
        return mapToTransactionResponse(t, payments.isEmpty() ? null : payments.get(0));
    }

    public byte[] exportTransactionsCsv(Long outletId, String status) {
        List<TransactionResponse> list = getTransactions(outletId, status);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM
        sb.append("No Transaksi,Tanggal,Kasir,Pelanggan,Subtotal,Diskon Nota,Voucher,Diskon Voucher,Pajak,Grand Total,Status\r\n");
        for (TransactionResponse t : list) {
            sb.append('"').append(escapeCsv(t.getTrxNo())).append("\",");
            sb.append('"').append(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "").append("\",");
            sb.append('"').append(escapeCsv(t.getCashierName())).append("\",");
            sb.append('"').append(escapeCsv(t.getCustomerName())).append("\",");
            sb.append(t.getSubtotal() != null ? t.getSubtotal() : 0.0).append(",");
            sb.append(t.getOrderDiscount() != null ? t.getOrderDiscount() : 0.0).append(",");
            sb.append('"').append(escapeCsv(t.getVoucherCode())).append("\",");
            sb.append(t.getVoucherDiscount() != null ? t.getVoucherDiscount() : 0.0).append(",");
            sb.append(t.getTax() != null ? t.getTax() : 0.0).append(",");
            sb.append(t.getGrandTotal() != null ? t.getGrandTotal() : 0.0).append(",");
            sb.append('"').append(escapeCsv(t.getStatus())).append("\"\r\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        return str.replace("\"", "\"\"");
    }

    private void deductStock(Long outletId, Long productId, Long variantId, int qty, String trxNo, User cashier) {
        Optional<Stock> stockOpt;
        if (variantId != null) {
            stockOpt = stockRepository.findByProductIdAndVariantIdAndOutletIdForUpdate(productId, variantId, outletId);
        } else {
            stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndOutletIdForUpdate(productId, outletId);
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
            stockOpt = stockRepository.findByProductIdAndVariantIdAndOutletIdForUpdate(productId, variantId, outletId);
        } else {
            stockOpt = stockRepository.findByProductIdAndVariantIdIsNullAndOutletIdForUpdate(productId, outletId);
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

        List<Payment> pList = paymentRepository.findByTrxId(t.getId());
        List<PaymentResponse> paymentDtos = pList.stream().map(p -> PaymentResponse.builder()
                .id(p.getId())
                .trxId(p.getTrxId())
                .paymentMethod(p.getPaymentMethod())
                .amount(p.getAmount())
                .status(p.getStatus())
                .reference(p.getReference())
                .dueDate(p.getDueDate())
                .paidAt(p.getPaidAt())
                .notes(p.getNotes())
                .build()).collect(Collectors.toList());

        String methodSummary = null;
        String statusSummary = null;
        Double totalPaid = 0.0;
        if (!pList.isEmpty()) {
            totalPaid = pList.stream().mapToDouble(Payment::getAmount).sum();
            if (pList.size() == 1) {
                methodSummary = pList.get(0).getPaymentMethod();
                statusSummary = pList.get(0).getStatus();
            } else {
                methodSummary = "SPLIT (" + pList.stream().map(Payment::getPaymentMethod).distinct().collect(Collectors.joining(", ")) + ")";
                boolean anyUnpaid = pList.stream().anyMatch(p -> "UNPAID".equalsIgnoreCase(p.getStatus()) || "PARTIAL".equalsIgnoreCase(p.getStatus()));
                boolean anyPaid = pList.stream().anyMatch(p -> "PAID".equalsIgnoreCase(p.getStatus()));
                if (anyUnpaid && anyPaid) statusSummary = "PARTIAL";
                else if (anyUnpaid) statusSummary = "UNPAID";
                else statusSummary = "PAID";
            }
        }

        double itemDiscountTotal = items.stream()
                .mapToDouble(i -> (i.getDiscount() != null ? i.getDiscount() : 0.0) * (i.getQuantity() != null ? i.getQuantity() : 1))
                .sum();

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
                .itemDiscountTotal(itemDiscountTotal)
                .orderDiscount(t.getOrderDiscount() != null ? t.getOrderDiscount() : 0.0)
                .orderDiscountType(t.getOrderDiscountType())
                .orderDiscountRate(t.getOrderDiscountRate())
                .voucherCode(t.getVoucherCode())
                .voucherDiscount(t.getVoucherDiscount() != null ? t.getVoucherDiscount() : 0.0)
                .tax(t.getTax())
                .grandTotal(t.getGrandTotal())
                .status(t.getStatus())
                .paymentMethod(methodSummary)
                .paymentAmount(totalPaid)
                .changeAmount(totalPaid >= t.getGrandTotal() ? totalPaid - t.getGrandTotal() : 0.0)
                .paymentStatus(statusSummary)
                .dueDate(t.getDueDate())
                .createdAt(t.getCreatedAt())
                .items(items)
                .payments(paymentDtos)
                .build();
    }

    private List<TransactionResponse> mapToTransactionResponseBatch(List<Transaction> list) {
        List<Long> trxIds = list.stream().map(Transaction::getId).collect(Collectors.toList());
        List<TransactionItem> allItems = transactionItemRepository.findByTrxIdIn(trxIds);
        Map<Long, List<TransactionItem>> itemsByTrx = allItems.stream()
                .collect(Collectors.groupingBy(TransactionItem::getTrxId));

        List<Payment> allPayments = paymentRepository.findByTrxIdIn(trxIds);
        Map<Long, List<Payment>> paymentsByTrx = allPayments.stream()
                .collect(Collectors.groupingBy(Payment::getTrxId));

        Set<Long> productIds = allItems.stream().map(TransactionItem::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> productNames = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        Set<Long> variantIds = allItems.stream().map(TransactionItem::getVariantId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> variantNames = productVariantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, ProductVariant::getName, (a, b) -> a));

        Set<Long> outletIds = list.stream().map(Transaction::getOutletId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> outletNames = outletRepository.findAllById(outletIds).stream()
                .collect(Collectors.toMap(Outlet::getId, Outlet::getName, (a, b) -> a));

        Set<Long> userIds = list.stream().map(Transaction::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> cashierNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        Set<Long> customerIds = list.stream().map(Transaction::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNames = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));

        return list.stream().map(t -> {
            String outletName = outletNames.get(t.getOutletId());
            String cashierName = cashierNames.get(t.getUserId());
            String customerName = t.getCustomerId() != null ? customerNames.getOrDefault(t.getCustomerId(), "Umum") : "Umum";

            List<CartItemDto> items = itemsByTrx.getOrDefault(t.getId(), Collections.emptyList()).stream()
                    .map(ti -> CartItemDto.builder()
                            .productId(ti.getProductId())
                            .variantId(ti.getVariantId())
                            .productName(productNames.get(ti.getProductId()))
                            .variantName(ti.getVariantId() != null ? variantNames.get(ti.getVariantId()) : null)
                            .quantity(ti.getQuantity())
                            .basePrice(ti.getBasePrice())
                            .unitPrice(ti.getUnitPrice())
                            .discount(ti.getDiscount())
                            .tax(ti.getTax())
                            .subtotal(ti.getSubtotal())
                            .build()
                    ).collect(Collectors.toList());

            List<Payment> pList = paymentsByTrx.getOrDefault(t.getId(), Collections.emptyList());
            List<PaymentResponse> paymentDtos = pList.stream().map(p -> PaymentResponse.builder()
                    .id(p.getId())
                    .trxId(p.getTrxId())
                    .paymentMethod(p.getPaymentMethod())
                    .amount(p.getAmount())
                    .status(p.getStatus())
                    .reference(p.getReference())
                    .dueDate(p.getDueDate())
                    .paidAt(p.getPaidAt())
                    .notes(p.getNotes())
                    .build()).collect(Collectors.toList());

            String methodSummary = null;
            String statusSummary = null;
            Double totalPaid = 0.0;
            if (!pList.isEmpty()) {
                totalPaid = pList.stream().mapToDouble(Payment::getAmount).sum();
                if (pList.size() == 1) {
                    methodSummary = pList.get(0).getPaymentMethod();
                    statusSummary = pList.get(0).getStatus();
                } else {
                    methodSummary = "SPLIT (" + pList.stream().map(Payment::getPaymentMethod).distinct().collect(Collectors.joining(", ")) + ")";
                    boolean anyUnpaid = pList.stream().anyMatch(p -> "UNPAID".equalsIgnoreCase(p.getStatus()) || "PARTIAL".equalsIgnoreCase(p.getStatus()));
                    boolean anyPaid = pList.stream().anyMatch(p -> "PAID".equalsIgnoreCase(p.getStatus()));
                    if (anyUnpaid && anyPaid) statusSummary = "PARTIAL";
                    else if (anyUnpaid) statusSummary = "UNPAID";
                    else statusSummary = "PAID";
                }
            }

            double batchItemDisc = items.stream()
                    .mapToDouble(i -> (i.getDiscount() != null ? i.getDiscount() : 0.0) * (i.getQuantity() != null ? i.getQuantity() : 1))
                    .sum();

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
                    .itemDiscountTotal(batchItemDisc)
                    .orderDiscount(t.getOrderDiscount() != null ? t.getOrderDiscount() : 0.0)
                    .orderDiscountType(t.getOrderDiscountType())
                    .orderDiscountRate(t.getOrderDiscountRate())
                    .voucherCode(t.getVoucherCode())
                    .voucherDiscount(t.getVoucherDiscount() != null ? t.getVoucherDiscount() : 0.0)
                    .tax(t.getTax())
                    .grandTotal(t.getGrandTotal())
                    .status(t.getStatus())
                    .paymentMethod(methodSummary)
                    .paymentAmount(totalPaid)
                    .changeAmount(totalPaid >= t.getGrandTotal() ? totalPaid - t.getGrandTotal() : 0.0)
                    .paymentStatus(statusSummary)
                    .dueDate(t.getDueDate())
                    .createdAt(t.getCreatedAt())
                    .items(items)
                    .payments(paymentDtos)
                    .build();
        }).collect(Collectors.toList());
    }

    private String generateTrxNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        return "TRX-" + timestamp + "-" + rand;
    }
}

