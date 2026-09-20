package com.zonapos.service;

import com.zonapos.dto.CloseShiftRequest;
import com.zonapos.dto.OpenShiftRequest;
import com.zonapos.dto.ShiftResponse;
import com.zonapos.entity.*;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final CashierShiftRepository cashierShiftRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final OutletRepository outletRepository;
    private final UserRepository userRepository;

    @Transactional
    public ShiftResponse openShift(OpenShiftRequest request, User cashier) {
        Optional<CashierShift> active = cashierShiftRepository.findByUserIdAndStatus(cashier.getId(), "OPEN");
        if (active.isPresent()) {
            throw new BadRequestException("Anda masih memiliki shift kasir yang aktif. Tutup shift terlebih dahulu.");
        }

        CashierShift shift = CashierShift.builder()
                .outletId(request.getOutletId())
                .userId(cashier.getId())
                .startCash(request.getStartCash())
                .expectedCash(request.getStartCash())
                .actualCash(0.0)
                .status("OPEN")
                .openedAt(LocalDateTime.now())
                .build();

        shift = cashierShiftRepository.save(shift);
        return mapToShiftResponse(shift);
    }

    @Transactional
    public ShiftResponse closeShift(Long shiftId, CloseShiftRequest request, User cashier) {
        CashierShift shift = cashierShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift tidak ditemukan dengan ID: " + shiftId));

        if (!"OPEN".equals(shift.getStatus())) {
            throw new BadRequestException("Shift ini sudah ditutup sebelumnya.");
        }

        // Calculate expected cash = startCash + Cash Transactions in this shift
        List<Transaction> transactions = transactionRepository.findByShiftId(shift.getId());
        double cashSales = 0.0;
        double nonCashSales = 0.0;
        int completedTrxCount = 0;

        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                completedTrxCount++;
                List<Payment> payments = paymentRepository.findByTrxId(trx.getId());
                for (Payment p : payments) {
                    if ("PAID".equals(p.getStatus())) {
                        if ("CASH".equalsIgnoreCase(p.getPaymentMethod())) {
                            cashSales += p.getAmount();
                        } else {
                            nonCashSales += p.getAmount();
                        }
                    }
                }
            }
        }

        double expectedCash = shift.getStartCash() + cashSales;
        shift.setExpectedCash(expectedCash);
        shift.setActualCash(request.getActualCash());
        shift.setStatus("CLOSED");
        shift.setClosedAt(LocalDateTime.now());
        shift = cashierShiftRepository.save(shift);

        ShiftResponse response = mapToShiftResponse(shift);
        response.setTotalCashSales(cashSales);
        response.setTotalNonCashSales(nonCashSales);
        response.setTotalTransactions(completedTrxCount);
        response.setCashDifference(request.getActualCash() - expectedCash);
        return response;
    }

    public ShiftResponse getActiveShift(Long userId) {
        return cashierShiftRepository.findByUserIdAndStatus(userId, "OPEN")
                .map(this::mapToShiftResponse)
                .orElse(null);
    }

    public List<ShiftResponse> getShiftsByOutlet(Long outletId) {
        return cashierShiftRepository.findByOutletIdOrderByOpenedAtDesc(outletId).stream()
                .map(this::mapToShiftResponse).collect(Collectors.toList());
    }

    private ShiftResponse mapToShiftResponse(CashierShift s) {
        String outletName = outletRepository.findById(s.getOutletId()).map(Outlet::getName).orElse(null);
        String cashierName = userRepository.findById(s.getUserId()).map(User::getName).orElse(null);

        // Calculate live sales if OPEN
        double cashSales = 0.0;
        double nonCashSales = 0.0;
        int completedTrxCount = 0;

        List<Transaction> transactions = transactionRepository.findByShiftId(s.getId());
        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                completedTrxCount++;
                List<Payment> payments = paymentRepository.findByTrxId(trx.getId());
                for (Payment p : payments) {
                    if ("PAID".equals(p.getStatus())) {
                        if ("CASH".equalsIgnoreCase(p.getPaymentMethod())) {
                            cashSales += p.getAmount();
                        } else {
                            nonCashSales += p.getAmount();
                        }
                    }
                }
            }
        }

        double expected = s.getStartCash() + cashSales;

        return ShiftResponse.builder()
                .id(s.getId())
                .outletId(s.getOutletId())
                .outletName(outletName)
                .userId(s.getUserId())
                .cashierName(cashierName)
                .startCash(s.getStartCash())
                .totalCashSales(cashSales)
                .totalNonCashSales(nonCashSales)
                .totalTransactions(completedTrxCount)
                .expectedCash(expected)
                .actualCash(s.getActualCash())
                .cashDifference(s.getActualCash() != null ? s.getActualCash() - expected : 0.0)
                .status(s.getStatus())
                .openedAt(s.getOpenedAt())
                .closedAt(s.getClosedAt())
                .build();
    }
}
