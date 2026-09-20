package com.zonapos.controller;

import com.zonapos.dto.CheckoutRequest;
import com.zonapos.dto.HoldOrderRequest;
import com.zonapos.dto.RefundRequest;
import com.zonapos.dto.TransactionResponse;
import com.zonapos.entity.User;
import com.zonapos.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/hold")
    public ResponseEntity<TransactionResponse> holdOrder(
            @Valid @RequestBody HoldOrderRequest request,
            @AuthenticationPrincipal User cashier) {
        return ResponseEntity.ok(transactionService.holdOrder(request, cashier));
    }

    @PostMapping("/checkout")
    public ResponseEntity<TransactionResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User cashier) {
        return ResponseEntity.ok(transactionService.checkout(request, cashier));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<TransactionResponse> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(transactionService.refund(id, request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam Long outletId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(transactionService.getTransactions(outletId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionDetail(id));
    }
}
