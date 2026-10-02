package com.zonapos.service;

import com.zonapos.dto.*;
import com.zonapos.entity.*;
import com.zonapos.exception.BadRequestException;
import com.zonapos.exception.ResourceNotFoundException;
import com.zonapos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final CashierShiftRepository cashierShiftRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final OutletRepository outletRepository;
    private final UserRepository userRepository;
    private final CashMovementRepository cashMovementRepository;

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
                .totalCashIn(0.0)
                .totalCashOut(0.0)
                .status("OPEN")
                .openedAt(LocalDateTime.now())
                .build();

        shift = cashierShiftRepository.save(shift);
        log.info("Shift opened: shiftId={}, cashierId={}, outletId={}, startCash={}",
                shift.getId(), cashier.getId(), shift.getOutletId(), shift.getStartCash());
        return mapToShiftResponse(shift);
    }

    @Transactional
    public CashMovementResponse recordCashMovement(CreateCashMovementRequest request, User user) {
        CashierShift shift;
        if (request.getShiftId() != null) {
            shift = cashierShiftRepository.findById(request.getShiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shift tidak ditemukan dengan ID: " + request.getShiftId()));
        } else {
            shift = cashierShiftRepository.findByUserIdAndStatus(user.getId(), "OPEN")
                    .orElseThrow(() -> new BadRequestException("Tidak ada shift kasir yang sedang aktif. Buka shift terlebih dahulu sebelum mencatat kas."));
        }

        if (!"OPEN".equals(shift.getStatus())) {
            throw new BadRequestException("Tidak dapat mencatat kas masuk/keluar pada shift yang sudah ditutup.");
        }

        String type = request.getType() != null ? request.getType().trim().toUpperCase() : "";
        if (!"CASH_IN".equals(type) && !"CASH_OUT".equals(type)) {
            throw new BadRequestException("Tipe mutasi kas harus CASH_IN (Kas Masuk) atau CASH_OUT (Kas Keluar).");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BadRequestException("Nominal uang harus lebih besar dari Rp 0.");
        }

        // Calculate live sales and existing movements
        double cashSales = calculateCashSales(shift.getId());
        double currentCashIn = Optional.ofNullable(cashMovementRepository.sumAmountByShiftIdAndType(shift.getId(), "CASH_IN")).orElse(0.0);
        double currentCashOut = Optional.ofNullable(cashMovementRepository.sumAmountByShiftIdAndType(shift.getId(), "CASH_OUT")).orElse(0.0);
        double currentDrawerBalance = shift.getStartCash() + cashSales + currentCashIn - currentCashOut;

        // Overdraft prevention: ensure drawer has enough physical money for CASH_OUT
        if ("CASH_OUT".equals(type) && request.getAmount() > currentDrawerBalance) {
            throw new BadRequestException(String.format(
                    "Uang kas di laci tidak mencukupi untuk pengeluaran ini. Saldo laci saat ini: Rp %,.0f, nominal kas keluar: Rp %,.0f",
                    currentDrawerBalance, request.getAmount()));
        }

        CashMovement movement = CashMovement.builder()
                .shiftId(shift.getId())
                .tenantId(user.getTenantId())
                .outletId(shift.getOutletId())
                .userId(user.getId())
                .type(type)
                .amount(request.getAmount())
                .category(request.getCategory() != null ? request.getCategory().trim() : "Operasional")
                .notes(request.getNotes() != null ? request.getNotes().trim() : "")
                .createdAt(LocalDateTime.now())
                .build();

        movement = cashMovementRepository.save(movement);

        // Update shift snapshot
        double updatedCashIn = "CASH_IN".equals(type) ? currentCashIn + request.getAmount() : currentCashIn;
        double updatedCashOut = "CASH_OUT".equals(type) ? currentCashOut + request.getAmount() : currentCashOut;
        double updatedExpected = shift.getStartCash() + cashSales + updatedCashIn - updatedCashOut;

        shift.setTotalCashIn(updatedCashIn);
        shift.setTotalCashOut(updatedCashOut);
        shift.setExpectedCash(updatedExpected);
        cashierShiftRepository.save(shift);

        log.info("Recorded cash movement: id={}, shiftId={}, type={}, amount={}, category={}, user={}",
                movement.getId(), shift.getId(), type, movement.getAmount(), movement.getCategory(), user.getName());

        return mapToMovementResponse(movement, user.getName());
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> getCashMovements(Long shiftId, User user) {
        CashierShift shift = cashierShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift tidak ditemukan dengan ID: " + shiftId));

        return cashMovementRepository.findByShiftIdOrderByCreatedAtDesc(shift.getId()).stream()
                .map(m -> mapToMovementResponse(m, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> getActiveShiftMovements(User user) {
        CashierShift shift = cashierShiftRepository.findByUserIdAndStatus(user.getId(), "OPEN")
                .orElseThrow(() -> new BadRequestException("Tidak ada shift kasir aktif saat ini."));

        return cashMovementRepository.findByShiftIdOrderByCreatedAtDesc(shift.getId()).stream()
                .map(m -> mapToMovementResponse(m, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public ShiftResponse closeShift(Long shiftId, CloseShiftRequest request, User cashier) {
        CashierShift shift = cashierShiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift tidak ditemukan dengan ID: " + shiftId));

        if (!"OPEN".equals(shift.getStatus())) {
            throw new BadRequestException("Shift ini sudah ditutup sebelumnya.");
        }

        // Calculate expected cash = startCash + Cash Transactions in this shift + CashIn - CashOut
        double cashSales = calculateCashSales(shift.getId());
        double nonCashSales = calculateNonCashSales(shift.getId());
        int completedTrxCount = countCompletedTransactions(shift.getId());

        double totalCashIn = Optional.ofNullable(cashMovementRepository.sumAmountByShiftIdAndType(shift.getId(), "CASH_IN")).orElse(0.0);
        double totalCashOut = Optional.ofNullable(cashMovementRepository.sumAmountByShiftIdAndType(shift.getId(), "CASH_OUT")).orElse(0.0);

        double expectedCash = shift.getStartCash() + cashSales + totalCashIn - totalCashOut;
        shift.setExpectedCash(expectedCash);
        shift.setTotalCashIn(totalCashIn);
        shift.setTotalCashOut(totalCashOut);
        shift.setActualCash(request.getActualCash());
        shift.setStatus("CLOSED");
        shift.setClosedAt(LocalDateTime.now());
        shift = cashierShiftRepository.save(shift);

        log.info("Shift closed: shiftId={}, expectedCash={}, actualCash={}, diff={}",
                shift.getId(), expectedCash, request.getActualCash(), request.getActualCash() - expectedCash);

        ShiftResponse response = mapToShiftResponse(shift);
        response.setTotalCashSales(cashSales);
        response.setTotalNonCashSales(nonCashSales);
        response.setTotalTransactions(completedTrxCount);
        response.setTotalCashIn(totalCashIn);
        response.setTotalCashOut(totalCashOut);
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

    private double calculateCashSales(Long shiftId) {
        double cashSales = 0.0;
        List<Transaction> transactions = transactionRepository.findByShiftId(shiftId);
        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                List<Payment> payments = paymentRepository.findByTrxId(trx.getId());
                for (Payment p : payments) {
                    if ("PAID".equals(p.getStatus()) && "CASH".equalsIgnoreCase(p.getPaymentMethod())) {
                        cashSales += p.getAmount();
                    }
                }
            }
        }
        return cashSales;
    }

    private double calculateNonCashSales(Long shiftId) {
        double nonCashSales = 0.0;
        List<Transaction> transactions = transactionRepository.findByShiftId(shiftId);
        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                List<Payment> payments = paymentRepository.findByTrxId(trx.getId());
                for (Payment p : payments) {
                    if ("PAID".equals(p.getStatus()) && !"CASH".equalsIgnoreCase(p.getPaymentMethod())) {
                        nonCashSales += p.getAmount();
                    }
                }
            }
        }
        return nonCashSales;
    }

    private int countCompletedTransactions(Long shiftId) {
        int count = 0;
        List<Transaction> transactions = transactionRepository.findByShiftId(shiftId);
        for (Transaction trx : transactions) {
            if ("COMPLETED".equals(trx.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private CashMovementResponse mapToMovementResponse(CashMovement m, String userName) {
        if (userName == null) {
            userName = userRepository.findById(m.getUserId()).map(User::getName).orElse("Kasir");
        }
        return CashMovementResponse.builder()
                .id(m.getId())
                .shiftId(m.getShiftId())
                .outletId(m.getOutletId())
                .userId(m.getUserId())
                .userName(userName)
                .type(m.getType())
                .amount(m.getAmount())
                .category(m.getCategory())
                .notes(m.getNotes())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private ShiftResponse mapToShiftResponse(CashierShift s) {
        String outletName = outletRepository.findById(s.getOutletId()).map(Outlet::getName).orElse(null);
        String cashierName = userRepository.findById(s.getUserId()).map(User::getName).orElse(null);

        double cashSales = calculateCashSales(s.getId());
        double nonCashSales = calculateNonCashSales(s.getId());
        int completedTrxCount = countCompletedTransactions(s.getId());

        double totalCashIn = Optional.ofNullable(cashMovementRepository.sumAmountByShiftIdAndType(s.getId(), "CASH_IN")).orElse(0.0);
        double totalCashOut = Optional.ofNullable(cashMovementRepository.sumAmountByShiftIdAndType(s.getId(), "CASH_OUT")).orElse(0.0);

        double expected = s.getStartCash() + cashSales + totalCashIn - totalCashOut;

        List<CashMovement> movements = cashMovementRepository.findByShiftIdOrderByCreatedAtDesc(s.getId());
        List<CashMovementResponse> movementResponses = movements.stream()
                .map(m -> mapToMovementResponse(m, null))
                .collect(Collectors.toList());

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
                .totalCashIn(totalCashIn)
                .totalCashOut(totalCashOut)
                .expectedCash(expected)
                .actualCash(s.getActualCash())
                .cashDifference(s.getActualCash() != null && !"OPEN".equals(s.getStatus()) ? s.getActualCash() - expected : 0.0)
                .status(s.getStatus())
                .openedAt(s.getOpenedAt())
                .closedAt(s.getClosedAt())
                .cashMovements(movementResponses)
                .build();
    }
}
