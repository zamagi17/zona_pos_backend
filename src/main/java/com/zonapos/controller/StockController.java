package com.zonapos.controller;

import com.zonapos.dto.StockAdjustmentRequest;
import com.zonapos.dto.StockPurchaseRequest;
import com.zonapos.dto.StockResponse;
import com.zonapos.dto.StockTransferRequest;
import com.zonapos.entity.StockHistory;
import com.zonapos.entity.User;
import com.zonapos.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/outlet/{outletId}")
    public ResponseEntity<List<StockResponse>> getStocksByOutlet(@PathVariable Long outletId) {
        return ResponseEntity.ok(stockService.getStocksByOutlet(outletId));
    }

    @GetMapping("/storage/{storageId}")
    public ResponseEntity<List<StockResponse>> getStocksByStorage(@PathVariable Long storageId) {
        return ResponseEntity.ok(stockService.getStocksByStorage(storageId));
    }

    @GetMapping("/low-stock/{outletId}")
    public ResponseEntity<List<StockResponse>> getLowStockAlert(@PathVariable Long outletId) {
        return ResponseEntity.ok(stockService.getLowStockAlert(outletId));
    }

    @GetMapping("/history/{stockId}")
    public ResponseEntity<List<StockHistory>> getStockHistory(@PathVariable Long stockId) {
        return ResponseEntity.ok(stockService.getStockHistory(stockId));
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<StockResponse> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(stockService.adjustStock(request, currentUser));
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<StockResponse> purchaseStock(
            @Valid @RequestBody StockPurchaseRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(stockService.purchaseStock(request, currentUser));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<?> transferStock(
            @Valid @RequestBody StockTransferRequest request,
            @AuthenticationPrincipal User currentUser) {
        stockService.transferStock(request, currentUser);
        return ResponseEntity.ok().build();
    }
}
