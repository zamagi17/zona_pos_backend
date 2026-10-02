package com.zonapos.controller;

import com.zonapos.dto.PromotionDto;
import com.zonapos.dto.PromotionValidationRequest;
import com.zonapos.dto.PromotionValidationResponse;
import com.zonapos.entity.User;
import com.zonapos.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionDto>> getActivePromotions(
            @RequestParam(required = false) Long outletId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(promotionService.getActivePromotions(currentUser.getTenantId(), outletId));
    }

    @PostMapping("/validate")
    public ResponseEntity<PromotionValidationResponse> validateVoucher(
            @Valid @RequestBody PromotionValidationRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(promotionService.validateVoucher(
                currentUser.getTenantId(),
                request.getOutletId(),
                request.getCode(),
                request.getSubtotal()
        ));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PromotionDto>> getAllPromotions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(promotionService.getAllPromotions(currentUser.getTenantId()));
    }

    @PostMapping
    public ResponseEntity<PromotionDto> createPromotion(
            @Valid @RequestBody PromotionDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(promotionService.createPromotion(dto, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionDto> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, dto, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        promotionService.deletePromotion(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<PromotionDto> togglePromotionStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(promotionService.togglePromotionStatus(id, currentUser));
    }
}
