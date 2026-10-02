package com.zonapos.controller;

import com.zonapos.dto.PurchaseOrderDto;
import com.zonapos.dto.SupplierDto;
import com.zonapos.entity.User;
import com.zonapos.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierDto>> getSuppliers(
            @RequestParam(required = false) Boolean onlyActive,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.getSuppliers(currentUser.getTenantId(), onlyActive));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDto> getSupplierById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.getSupplierById(id, currentUser.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<SupplierDto> createSupplier(
            @Valid @RequestBody SupplierDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.createSupplier(dto, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, dto, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<Map<String, String>> deleteSupplier(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        supplierService.deleteSupplier(id, currentUser.getTenantId());
        return ResponseEntity.ok(Map.of("message", "Supplier berhasil dihapus"));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<SupplierDto> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.toggleStatus(id, currentUser.getTenantId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PurchaseOrderDto>> getPurchaseHistory(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.getPurchaseHistory(currentUser.getTenantId(), productId, variantId));
    }

    @GetMapping("/last-price")
    public ResponseEntity<Map<String, Object>> getLastPurchasePrice(
            @RequestParam Long productId,
            @RequestParam(required = false) Long variantId,
            @AuthenticationPrincipal User currentUser) {
        Double lastPrice = supplierService.getLastPurchasePrice(currentUser.getTenantId(), productId, variantId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "variantId", variantId != null ? variantId : 0,
                "lastPurchasePrice", lastPrice != null ? lastPrice : 0.0
        ));
    }

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<List<PurchaseOrderDto>> getAllPurchaseOrders(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long storageId,
            @RequestParam(required = false) Long outletId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(supplierService.getAllPurchaseOrders(
                currentUser.getTenantId(), supplierId, storageId, outletId, startDate, endDate, search
        ));
    }
}
