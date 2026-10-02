package com.zonapos.service;

import com.zonapos.dto.*;
import com.zonapos.entity.Customer;
import com.zonapos.entity.Payment;
import com.zonapos.entity.Transaction;
import com.zonapos.entity.User;
import com.zonapos.entity.CashierShift;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.CustomerDebtSummary;
import com.zonapos.repository.CustomerRepository;
import com.zonapos.repository.PaymentRepository;
import com.zonapos.repository.TransactionRepository;
import com.zonapos.repository.CashierShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final PaymentRepository paymentRepository;
    private final CashierShiftRepository cashierShiftRepository;
    private final ShiftService shiftService;

    public List<CustomerDto> getCustomersByTenant(Long tenantId) {
        List<Customer> customers = customerRepository.findByTenantId(tenantId);
        if (customers.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch debt summaries to prevent N+1 queries
        Map<Long, CustomerDebtSummary> debtMap = paymentRepository.findReceivablesSummaryByTenantId(tenantId).stream()
                .collect(Collectors.toMap(CustomerDebtSummary::getCustomerId, s -> s, (a, b) -> a));

        return customers.stream()
                .map(c -> {
                    CustomerDto dto = mapToDto(c);
                    CustomerDebtSummary summary = debtMap.get(c.getId());
                    dto.setTotalReceivables(summary != null && summary.getTotalDebt() != null ? summary.getTotalDebt() : 0.0);
                    dto.setUnpaidBillsCount(summary != null && summary.getUnpaidCount() != null ? summary.getUnpaidCount() : 0L);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public CustomerDto getCustomerById(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pelanggan tidak ditemukan"));
        CustomerDto dto = mapToDto(c);
        Double totalDebt = paymentRepository.sumUnpaidAmountByCustomerId(id);
        List<Payment> unpaid = paymentRepository.findUnpaidByCustomerId(id);
        dto.setTotalReceivables(totalDebt != null ? totalDebt : 0.0);
        dto.setUnpaidBillsCount((long) unpaid.size());
        return dto;
    }

    @Transactional
    public CustomerDto createCustomer(CustomerDto dto, User currentUser) {
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            if (customerRepository.findByTenantIdAndPhone(currentUser.getTenantId(), dto.getPhone()).isPresent()) {
                throw new BadRequestException("Nomor telepon pelanggan sudah terdaftar");
            }
        }

        Customer customer = Customer.builder()
                .tenantId(currentUser.getTenantId())
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .build();
        customer = customerRepository.save(customer);
        CustomerDto res = mapToDto(customer);
        res.setTotalReceivables(0.0);
        res.setUnpaidBillsCount(0L);
        return res;
    }

    public List<TransactionResponse> getCustomerTransactions(Long customerId) {
        List<Transaction> transactions = transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return transactions.stream()
                .map(t -> transactionService.getTransactionDetail(t.getId()))
                .collect(Collectors.toList());
    }

    public List<UnpaidBillResponse> getUnpaidBills(Long customerId) {
        List<Payment> unpaidPayments = paymentRepository.findUnpaidByCustomerId(customerId);
        if (unpaidPayments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> trxIds = unpaidPayments.stream().map(Payment::getTrxId).distinct().collect(Collectors.toList());
        Map<Long, Transaction> trxMap = transactionRepository.findAllById(trxIds).stream()
                .collect(Collectors.toMap(Transaction::getId, t -> t));

        return unpaidPayments.stream().map(p -> {
            Transaction trx = trxMap.get(p.getTrxId());
            return UnpaidBillResponse.builder()
                    .paymentId(p.getId())
                    .trxId(p.getTrxId())
                    .trxNo(trx != null ? trx.getTrxNo() : "-")
                    .trxDate(trx != null ? trx.getCreatedAt() : p.getCreatedAt())
                    .grandTotal(trx != null ? trx.getGrandTotal() : p.getAmount())
                    .remainingAmount(p.getAmount())
                    .dueDate(p.getDueDate() != null ? p.getDueDate() : (trx != null ? trx.getDueDate() : null))
                    .notes(p.getNotes())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional
    public CustomerDto settleDebt(Long customerId, SettleDebtRequest request, User currentUser) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Pelanggan tidak ditemukan dengan ID: " + customerId));

        if (!customer.getTenantId().equals(currentUser.getTenantId())) {
            throw new BadRequestException("Pelanggan tidak valid untuk tenant ini.");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BadRequestException("Nominal pelunasan kasbon harus lebih besar dari Rp 0.");
        }

        List<Payment> unpaidPayments;
        if (request.getTrxId() != null) {
            unpaidPayments = paymentRepository.findUnpaidByCustomerIdAndTrxId(customerId, request.getTrxId());
        } else {
            unpaidPayments = paymentRepository.findUnpaidByCustomerId(customerId);
        }

        if (unpaidPayments.isEmpty()) {
            throw new BadRequestException("Tidak ada tagihan piutang / kasbon aktif untuk pelanggan ini.");
        }

        double remainingToSettle = request.getAmount();
        double totalActuallySettled = 0.0;
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod().trim().toUpperCase() : "CASH";

        for (Payment p : unpaidPayments) {
            if (remainingToSettle <= 0) break;
            double currentDebt = p.getAmount();

            if (remainingToSettle >= currentDebt) {
                p.setStatus("PAID");
                p.setPaymentMethod(method);
                p.setPaidAt(LocalDateTime.now());
                String noteSuffix = "Lunas via " + method +
                        (request.getNotes() != null && !request.getNotes().isBlank() ? " (" + request.getNotes() + ")" : "");
                p.setNotes(p.getNotes() != null && !p.getNotes().isBlank() ? p.getNotes() + " | " + noteSuffix : noteSuffix);
                p.setUpdatedBy(currentUser.getName());
                paymentRepository.save(p);

                remainingToSettle -= currentDebt;
                totalActuallySettled += currentDebt;
            } else {
                p.setAmount(currentDebt - remainingToSettle);
                p.setStatus("PARTIAL");
                String noteSuffix = "Cicilan Rp " + String.format("%,.0f", remainingToSettle) + " via " + method;
                p.setNotes(p.getNotes() != null && !p.getNotes().isBlank() ? p.getNotes() + " | " + noteSuffix : noteSuffix);
                p.setUpdatedBy(currentUser.getName());
                paymentRepository.save(p);

                Payment settledPartial = Payment.builder()
                        .trxId(p.getTrxId())
                        .paymentMethod(method)
                        .amount(remainingToSettle)
                        .status("PAID")
                        .paidAt(LocalDateTime.now())
                        .notes("Cicilan pelunasan kasbon: " + (request.getNotes() != null ? request.getNotes() : ""))
                        .createdBy(currentUser.getName())
                        .build();
                paymentRepository.save(settledPartial);

                totalActuallySettled += remainingToSettle;
                remainingToSettle = 0.0;
            }
        }

        // Cash Drawer Integration: if paid via CASH and user has an active shift, record CASH_IN
        if ("CASH".equalsIgnoreCase(method) && totalActuallySettled > 0) {
            Optional<CashierShift> activeShift = cashierShiftRepository.findByUserIdAndStatus(currentUser.getId(), "OPEN");
            if (activeShift.isPresent()) {
                shiftService.recordCashMovement(CreateCashMovementRequest.builder()
                        .shiftId(activeShift.get().getId())
                        .type("CASH_IN")
                        .amount(totalActuallySettled)
                        .category("Pelunasan Kasbon")
                        .notes("Pelunasan kasbon pelanggan " + customer.getName() +
                                (request.getNotes() != null && !request.getNotes().isBlank() ? " - " + request.getNotes() : ""))
                        .build(), currentUser);
            }
        }

        log.info("Settled customer debt: customerId={}, tenantId={}, settledAmount={}, method={}, user={}",
                customerId, currentUser.getTenantId(), totalActuallySettled, method, currentUser.getName());

        CustomerDto res = mapToDto(customer);
        Double updatedDebt = paymentRepository.sumUnpaidAmountByCustomerId(customerId);
        List<Payment> remainingUnpaid = paymentRepository.findUnpaidByCustomerId(customerId);
        res.setTotalReceivables(updatedDebt != null ? updatedDebt : 0.0);
        res.setUnpaidBillsCount((long) remainingUnpaid.size());
        return res;
    }

    private CustomerDto mapToDto(Customer c) {
        return CustomerDto.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
