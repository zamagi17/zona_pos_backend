package com.zonapos.controller;

import com.zonapos.dto.ProductDto;
import com.zonapos.dto.ProductVariantDto;
import com.zonapos.entity.User;
import com.zonapos.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long outletId) {
        Long targetOutlet = outletId != null ? outletId : currentUser.getOutletId();
        return ResponseEntity.ok(productService.getProducts(currentUser.getTenantId(), targetOutlet));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long outletId) {
        Long targetOutlet = outletId != null ? outletId : currentUser.getOutletId();
        return ResponseEntity.ok(productService.getProductById(id, targetOutlet));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(productService.createProduct(dto, currentUser));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'OUTLET_MANAGER')")
    public ResponseEntity<ProductVariantDto> addVariant(
            @PathVariable Long id,
            @Valid @RequestBody ProductVariantDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(productService.addVariant(id, dto, currentUser));
    }
}
