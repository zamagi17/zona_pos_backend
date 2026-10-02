package com.zonapos.controller;

import com.zonapos.dto.ReceiptSettingDto;
import com.zonapos.entity.User;
import com.zonapos.service.ReceiptSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/receipt-settings")
@RequiredArgsConstructor
public class ReceiptSettingController {

    private final ReceiptSettingService receiptSettingService;

    @GetMapping
    public ResponseEntity<ReceiptSettingDto> getReceiptSetting(
            @RequestParam(required = false) Long outletId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptSettingService.getReceiptSetting(currentUser.getTenantId(), outletId));
    }

    @PostMapping
    public ResponseEntity<ReceiptSettingDto> saveReceiptSetting(
            @Valid @RequestBody ReceiptSettingDto dto,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(receiptSettingService.saveReceiptSetting(dto, currentUser));
    }
}
